/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package com.intellij.openapi.progress.impl;

import com.intellij.codeWithMe.ClientId;
import com.intellij.concurrency.ContextAwareRunnable;
import com.intellij.diagnostic.ThreadDumper;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.WriteIntentReadAction;
import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.application.ex.ApplicationManagerEx;
import com.intellij.openapi.application.ex.ApplicationUtil;
import com.intellij.openapi.diagnostic.Attachment;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.*;
import com.intellij.openapi.progress.util.TitledIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.*;
import com.intellij.openapi.wm.ex.ProgressIndicatorEx;
import com.intellij.util.ExceptionUtil;
import com.intellij.util.Java11Shim;
import com.intellij.util.SystemProperties;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.containers.ConcurrentLongObjectMap;
import com.intellij.util.ui.EDT;
import org.jetbrains.annotations.*;

import javax.swing.*;
import java.io.StringWriter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;

import static com.intellij.openapi.application.ModalityKt.currentThreadContextModality;
import static com.intellij.openapi.progress.impl.ProgressManagerScopeKt.ProgressManagerScope;

/**
 * This class is a simplified version of the original one, which tries to avoid loading a specific class
 * kotlinx/coroutines/internal/intellij/IntellijCoroutine from kotlinx.coroutines fork in a CLI environment.
 *
 * After IJPL-207644 has been fixed, since 253 we can hopefully remove the workaround, via setting
 * `ide.can.use.coroutines.fork` property to false.
 *
 * TODO: Remove it once KT-81457 is fixed
 */
public class CoreProgressManager extends ProgressManager implements Disposable {
  private static final Logger LOG = Logger.getInstance(CoreProgressManager.class);

  @ApiStatus.Internal
  public static final int CHECK_CANCELED_DELAY_MILLIS = 10;
  private final AtomicInteger myUnsafeProgressCount = new AtomicInteger(0);

  private ScheduledFuture<?> myCheckCancelledFuture; // guarded by threadsUnderIndicator

  // indicator -> threads which are running under this indicator.
  // THashMap is avoided here because of tombstones overhead
  private static final Map<ProgressIndicator, Set<Thread>> threadsUnderIndicator = new HashMap<>(); // guarded by threadsUnderIndicator
  // the active indicator for the thread id
  private static final ConcurrentLongObjectMap<ProgressIndicator> currentIndicators =
    Java11Shim.Companion.getINSTANCE().createConcurrentLongObjectMap();
  // top-level indicators for the thread id
  private static final ConcurrentLongObjectMap<ProgressIndicator> threadTopLevelIndicators =
    Java11Shim.Companion.getINSTANCE().createConcurrentLongObjectMap();
  // threads which are running under canceled indicator
  private static final Set<Thread> threadsUnderCanceledIndicator = new HashSet<>(); // guarded by threadsUnderIndicator

  @TestOnly
  @ApiStatus.Internal
  public static boolean hasThreadUnderCanceledIndicator(@NotNull Thread thread) {
   return threadsUnderCanceledIndicator.contains(thread);
  }

  private static volatile @NotNull CheckCanceledBehavior ourCheckCanceledBehavior = CheckCanceledBehavior.NONE;

  private enum CheckCanceledBehavior {
    /**
     * Nothing to be executed during ProgressManager.checkCanceled
     */
    NONE,
    /**
     * At least one hook exists and should be executed during ProgressManager.checkCanceled
     */
    ONLY_HOOKS,
    /**
     * There is at least one indicator in the canceled state,
     * during ProgressManager.checkCanceled the processes underneath are to be canceled, and all existing hooks are to be executed
     */
    INDICATOR_PLUS_HOOKS
  }

  /**
   * active (i.e., which have {@link #executeProcessUnderProgress(Runnable, ProgressIndicator)} method running) indicators
   * which are not inherited from {@link StandardProgressIndicator}.
   * for them an extra processing thread (see {@link #myCheckCancelledFuture}) has to be run
   * to call their non-standard {@link ProgressIndicator#checkCanceled()} method periodically.
   * Poor-man Multiset here (instead of a set) is for simplifying add/remove indicators on process-with-progress start/end with possibly identical indicators.
   * ProgressIndicator -> count of this indicator occurrences in this multiset.
   */
  private static final Map<ProgressIndicator, AtomicInteger> nonStandardIndicators = new ConcurrentHashMap<>();

  // must be under threadsUnderIndicator lock
  private void startBackgroundNonStandardIndicatorsPing() {
    if (myCheckCancelledFuture != null) {
      return;
    }

    myCheckCancelledFuture = AppExecutorUtil.getAppScheduledExecutorService().scheduleWithFixedDelay(() -> {
      for (ProgressIndicator indicator : nonStandardIndicators.keySet()) {
        try {
          indicator.checkCanceled();
        }
        catch (ProcessCanceledException e) {
          indicatorCanceled(indicator);
        }
      }
    }, 0, CHECK_CANCELED_DELAY_MILLIS, TimeUnit.MILLISECONDS);
  }

  // must be under threadsUnderIndicator lock
  private void stopBackgroundNonStandardIndicatorsPing() {
    if (myCheckCancelledFuture != null) {
      myCheckCancelledFuture.cancel(true);
      myCheckCancelledFuture = null;
    }
  }

  @Override
  public void dispose() {
    synchronized (threadsUnderIndicator) {
      stopBackgroundNonStandardIndicatorsPing();
    }
  }

  @ApiStatus.Internal
  public static @NotNull List<ProgressIndicator> getCurrentIndicators() {
    synchronized (threadsUnderIndicator) {
      return new ArrayList<>(threadsUnderIndicator.keySet());
    }
  }

  @ApiStatus.Internal
  public boolean runCheckCanceledHooks(@Nullable ProgressIndicator indicator) {
    return false;
  }
  @ApiStatus.Internal
  protected boolean hasCheckCanceledHooks() {
    return false;
  }

  @Override
  protected void doCheckCanceled() throws ProcessCanceledException {
      // Do nothing
  }

  @Override
  public boolean hasProgressIndicator() {
    return getProgressIndicator() != null;
  }

  @Override
  public boolean hasUnsafeProgressIndicator() {
    return myUnsafeProgressCount.get() > 0;
  }

  @Override
  public boolean hasModalProgressIndicator() {
    synchronized (threadsUnderIndicator) {
      for (ProgressIndicator t : threadsUnderIndicator.keySet()) {
        if (t.isModal()) {
          return true;
        }
      }
      return false;
    }
  }

  // run in current thread
  @Override
  public void runProcess(@NotNull Runnable process, @Nullable ProgressIndicator progress) {
    if (progress != null) {
      assertNoOtherThreadUnder(progress);
    }
    executeProcessUnderProgress(() -> {
      try {
        try {
          if (progress != null && !progress.isRunning()) {
            progress.start();
          }
        }
        catch (RuntimeException e) {
          throw e;
        }
        catch (Throwable e) {
          throw new RuntimeException(e);
        }
        process.run();
        logProcessIndicator(progress, false);
      }
      finally {
        if (progress != null && progress.isRunning()) {
          progress.stop();
          if (progress instanceof ProgressIndicatorEx) {
            ((ProgressIndicatorEx)progress).processFinish();
          }
        }
      }
    }, progress);
  }

  private static String getProgressIndicatorText(@Nullable ProgressIndicator progress) {
    if (!(progress instanceof TitledIndicator)) {
      return null;
    }
    return  ((TitledIndicator)progress).getTitle();
  }

  private static void logProcessIndicator(@Nullable ProgressIndicator progress, Boolean started) {
    String progressText = getProgressIndicatorText(progress);
    if (progressText == null) return;
    if (ApplicationManagerEx.isInIntegrationTest()) {
      LOG.info("Progress indicator:" + (started ? "started" : "finished") + ":" + progressText);
    }
  }

  private static void assertNoOtherThreadUnder(@NotNull ProgressIndicator progress) {
    synchronized (threadsUnderIndicator) {
      Collection<Thread> threads = threadsUnderIndicator.get(progress);
      Thread other = threads == null || threads.isEmpty() ? null : threads.iterator().next();
      if (other != null) {
        if (other == Thread.currentThread()) {
          LOG.error("This thread is already running under this indicator, starting/stopping it here might be a data race");
        }
        else {
          StringWriter stackTrace = new StringWriter();
          ThreadDumper.dumpCallStack(other, stackTrace, other.getStackTrace());
          LOG.error("Other (" + other +") is already running under this indicator (" + progress+ ", " + progress.getClass() + "), starting/stopping it here might be a data race.\n" +
                    "Consider using com.intellij.openapi.progress.ProgressManager.executeProcessUnderProgress\n" +
                    "The other stack trace:\n" + stackTrace);
        }
      }
    }
  }

  // run in the current thread (?)
  @Override
  public void executeNonCancelableSection(@NotNull Runnable runnable) {
    computeInNonCancelableSection(() -> {
      runnable.run();
      return null;
    });
  }

  // FROM EDT: bg OR calling if can't
  @Override
  public <T, E extends Exception> T computeInNonCancelableSection(@NotNull ThrowableComputable<T, E> computable) throws E {
    return computable.compute();
  }

  @Override
  public boolean runProcessWithProgressSynchronously(@NotNull Runnable process,
                                                     @NotNull @NlsContexts.DialogTitle String progressTitle,
                                                     boolean canBeCanceled,
                                                     @Nullable Project project) {
    return runProcessWithProgressSynchronously(process, progressTitle, canBeCanceled, project, null);
  }

  @Override
  public <T, E extends Exception> T runProcessWithProgressSynchronously(@NotNull ThrowableComputable<T, E> process,
                                                                        @NotNull @NlsContexts.DialogTitle String progressTitle,
                                                                        boolean canBeCanceled,
                                                                        @Nullable Project project) throws E {
    AtomicReference<T> result = new AtomicReference<>();
    AtomicReference<Throwable> exception = new AtomicReference<>();
    runProcessWithProgressSynchronously(new Task.Modal(project, progressTitle, canBeCanceled) {
      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        try {
          T compute = process.compute();
          result.set(compute);
        }
        catch (Throwable t) {
          exception.set(t);
        }
      }
    });

    Throwable t = exception.get();
    if (t != null) {
      ExceptionUtil.rethrowUnchecked(t);
      @SuppressWarnings("unchecked") E e = (E)t;
      throw e;
    }

    return result.get();
  }

  // FROM EDT: bg OR calling if can't
  @Override
  public boolean runProcessWithProgressSynchronously(@NotNull Runnable process,
                                                     @NotNull @NlsContexts.DialogTitle String progressTitle,
                                                     boolean canBeCanceled,
                                                     @Nullable Project project,
                                                     @Nullable JComponent parentComponent) {
    Task.Modal task = new Task.Modal(project, parentComponent, progressTitle, canBeCanceled) {
      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        process.run();
      }
    };
    return runProcessWithProgressSynchronously(task);
  }

  // bg; runnables on UI/EDT?
  @SuppressWarnings({"UnstableApiUsage", "deprecation"})
  @Override
  public void runProcessWithProgressAsynchronously(@NotNull Project project,
                                                   @NotNull @NlsContexts.ProgressTitle String progressTitle,
                                                   @NotNull Runnable process,
                                                   @Nullable Runnable successRunnable,
                                                   @Nullable Runnable canceledRunnable,
                                                   @NotNull PerformInBackgroundOption option) {
    runProcessWithProgressAsynchronously(new Task.Backgroundable(project, progressTitle, true, option) {
      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        process.run();
      }


      @Override
      public void onCancel() {
        if (canceledRunnable != null) {
          canceledRunnable.run();
        }
      }

      @Override
      public void onSuccess() {
        if (successRunnable != null) {
          successRunnable.run();
        }
      }
    });
  }

  /**
   * Different places in IntelliJ codebase behaves differently in case of headless mode.
   * <p>
   * Often, they're trying to make async parts synchronous to make it more predictable or controllable.
   * E.g., in tests or IntelliJ-based command line tools this is the usual code:
   * <p>
   * ```
   * if (ApplicationManager.getApplication().isHeadless()) {
   * performSyncChange()
   * }
   * else {
   * scheduleAsyncChange()
   * }
   * ```
   * <p>
   * However, sometimes headless application should behave just as regular GUI Application,
   * with all its asynchronous stuff. For that, the application must declare `intellij.progress.task.ignoreHeadless`
   * system property. And clients should modify its pure `isHeadless` condition to something like
   * <p>
   * ```
   * ApplicationManager.getApplication().isHeadless() && !shouldRunHeadlessTasksAsynchronously()
   * ```
   *
   * @return true is asynchronous tasks must remain asynchronous even in headless mode
   */
  @ApiStatus.Internal
  public static boolean shouldKeepTasksAsynchronousInHeadlessMode() {
    return SystemProperties.getBooleanProperty("intellij.progress.task.ignoreHeadless", false);
  }

  @ApiStatus.Internal
  public static boolean shouldKeepTasksAsynchronous() {
    Application application = ApplicationManager.getApplication();
    boolean isHeadless = application.isUnitTestMode() || application.isHeadlessEnvironment();
    return !isHeadless || shouldKeepTasksAsynchronousInHeadlessMode();
  }

  // from any: bg or current if can't
  @Override
  public void run(@NotNull Task task) {
    if (isSynchronousHeadless(task)) {
      if (SwingUtilities.isEventDispatchThread()) {
        WriteIntentReadAction.run((Runnable)() -> runProcessWithProgressSynchronously(task));
      }
      else {
        runProcessWithProgressInCurrentThread(task, new EmptyProgressIndicator(), ModalityState.defaultModalityState());
      }
    }
    else if (task.isModal()) {
      runProcessWithProgressSynchronously(task.asModal());
    }
    else {
      Task.Backgroundable backgroundable = task.asBackgroundable();
      if (isSynchronous(backgroundable)) {
        runProcessWithProgressSynchronously(backgroundable);
      }
      else {
        runAsynchronously(backgroundable);
      }
    }
  }

  private static boolean isSynchronousHeadless(Task task) {
    return task.isHeadless() && !shouldKeepTasksAsynchronousInHeadlessMode();
  }

  private static boolean isSynchronous(Task.Backgroundable backgroundable) {
    return backgroundable.isConditionalModal() && !backgroundable.shouldStartInBackground();
  }

  // from any: bg
  private void runAsynchronously(@NotNull Task.Backgroundable task) {
    if (LOG.isDebugEnabled()) LOG.debug("CoreProgressManager#runAsynchronously, " + task, new Throwable());
    if (EDT.isCurrentThreadEdt()) {
      runProcessWithProgressAsynchronously(task);
    }
    else {
      ApplicationManager.getApplication().invokeLater(() -> {
        Project project = task.getProject();
        if (project != null && project.isDisposed()) {
          LOG.info("Task canceled because of project disposal: " + task);
          finishTask(task, true, null);
          return;
        }

        runProcessWithProgressAsynchronously(task);
      }, ModalityState.defaultModalityState());
    }
  }

  protected @NotNull ProgressIndicator createDefaultAsynchronousProgressIndicator(@NotNull Task.Backgroundable task) {
    return new EmptyProgressIndicator();
  }

  // from any: bg
  public @NotNull Future<?> runProcessWithProgressAsynchronously(@NotNull Task.Backgroundable task) {
    return runProcessWithProgressAsynchronously(task, createDefaultAsynchronousProgressIndicator(task), null);
  }

  // from any: bg
  public @NotNull Future<?> runProcessWithProgressAsynchronously(@NotNull Task.Backgroundable task,
                                                        @NotNull ProgressIndicator progressIndicator,
                                                        @Nullable Runnable continuation) {
    return runProcessWithProgressAsynchronously(task, progressIndicator, continuation, progressIndicator.getModalityState());
  }

  @Deprecated
  protected void startTask(@NotNull Task task, @NotNull ProgressIndicator indicator, @Nullable Runnable continuation) {
    try {
      if (LOG.isDebugEnabled()) LOG.debug("Starting task '" + task + "' under progress: " + indicator, new Throwable());
      task.run(indicator);
    }
    finally {
      try {
        if (indicator instanceof ProgressIndicatorEx) {
          ((ProgressIndicatorEx)indicator).finish(task);
        }
      }
      finally {
        if (continuation != null) {
          continuation.run();
        }
      }
    }
  }

  private static class IndicatorDisposable implements Disposable {
    private final @NotNull ProgressIndicator myIndicator;

    IndicatorDisposable(@NotNull ProgressIndicator indicator) {
      myIndicator = indicator;
    }

    @Override
    public void dispose() {
      // do nothing if already disposed
      Disposer.dispose((Disposable)myIndicator, false);
    }
  }

  // from any: bg, task.finish on "UI/EDT"
  @SuppressWarnings({"deprecation", "UnstableApiUsage"})
  public @NotNull Future<?> runProcessWithProgressAsynchronously(@NotNull Task.Backgroundable task,
                                                        @NotNull ProgressIndicator progressIndicator,
                                                        @Nullable Runnable continuation,
                                                        @NotNull ModalityState modalityState) {
    IndicatorDisposable indicatorDisposable;
    if (progressIndicator instanceof Disposable) {
      // use IndicatorDisposable instead of progressIndicator to
      // avoid re-registering progressIndicator if it was registered on some other parent before
      indicatorDisposable = new IndicatorDisposable(progressIndicator);
      Disposer.register(ApplicationManager.getApplication(), indicatorDisposable);
    }
    else {
      indicatorDisposable = null;
    }

    AtomicLong elapsed = new AtomicLong();
    return new ProgressRunner<>(progress -> {
      long start = System.currentTimeMillis();
      try {
        startTask(task, progress, continuation);
      }
      finally {
        elapsed.set(System.currentTimeMillis() - start);
      }
      return null;
    }).onThread(ProgressRunner.ThreadToUse.POOLED)
      .withProgress(progressIndicator)
      .submit()
      .whenComplete(ClientId.decorateBiConsumer((result, err) -> {
        if (!result.isCanceled()) {
          notifyTaskFinished(task, elapsed.get());
        }

        ApplicationUtil.invokeLaterSomewhere(task.whereToRunCallbacks(), modalityState, () -> {
          try {
            finishTask(task, result.isCanceled(), result.getThrowable() instanceof ProcessCanceledException ? null : result.getThrowable());
          }
          finally {
            if (indicatorDisposable != null) {
              Disposer.dispose(indicatorDisposable);
            }
          }
        });
      }));
  }

  @ApiStatus.Internal
  public void notifyTaskFinished(@NotNull Task.Backgroundable task, long elapsed) {
  }

  // ASSERT IS EDT->UI bg or calling if can't
  // NEW: no assert; bg or calling ...
  @SuppressWarnings("deprecation")
  protected boolean runProcessWithProgressSynchronously(@NotNull Task task) {
    if (LOG.isDebugEnabled()) LOG.debug("CoreProgressManager#runProcessWithProgressSynchronously, " + task, new Throwable());
    Ref<Throwable> exceptionRef = new Ref<>();
    Runnable taskContainer = () -> {
      try {
        startTask(task, getProgressIndicator(), null);
      }
      catch (ProcessCanceledException e) {
        throw e;
      }
      catch (Throwable e) {
        exceptionRef.set(e);
      }
    };

    ApplicationEx application = ApplicationManagerEx.getApplicationEx();
    boolean result = application.runProcessWithProgressSynchronously(taskContainer,
                                                                     task.getTitle(),
                                                                     task.isCancellable(),
                                                                     shouldEnterModalityState(task),
                                                                     task.getProject(),
                                                                     task.getParentComponent(),
                                                                     task.getCancelText());

    ApplicationUtil.invokeAndWaitSomewhere(task.whereToRunCallbacks(),
                                           application.getDefaultModalityState(),
                                           () -> finishTask(task, !result, exceptionRef.get()));
    return result;
  }

  @ApiStatus.Internal
  @VisibleForTesting
  public static boolean shouldEnterModalityState(@NotNull Task task) {
    return task.isModal() ||
           EDT.isCurrentThreadEdt() &&
           !isSynchronousHeadless(task) &&
           isSynchronous(task.asBackgroundable());
  }

  @SuppressWarnings("deprecation")
  public void runProcessWithProgressInCurrentThread(@NotNull Task task,
                                                    @NotNull ProgressIndicator progressIndicator,
                                                    @NotNull ModalityState modalityState) {
    if (LOG.isDebugEnabled()) LOG.debug("CoreProgressManager#runProcessWithProgressInCurrentThread, " + task, new Throwable());
    if (progressIndicator instanceof Disposable) {
      Disposer.register(ApplicationManager.getApplication(), (Disposable)progressIndicator);
    }

    boolean processCanceled = false;
    Throwable exception = null;
    try {
      runProcess(() -> startTask(task, progressIndicator, null), progressIndicator);
    }
    catch (ProcessCanceledException e) {
      processCanceled = true;
    }
    catch (Throwable e) {
      exception = e;
    }

    boolean finalCanceled = processCanceled || progressIndicator.isCanceled();
    Throwable finalException = exception;

    ApplicationUtil.invokeAndWaitSomewhere(task.whereToRunCallbacks(), modalityState, () -> finishTask(task, finalCanceled, finalException));
  }

  protected void finishTask(@NotNull Task task, boolean canceled, @Nullable Throwable error) {
    try {
      if (error != null) {
        task.onThrowable(error);
      }
      else if (canceled) {
        task.onCancel();
      }
      else {
        task.onSuccess();
      }
    }
    finally {
      task.onFinished();
    }
  }

  // bg
  @Override
  public void runProcessWithProgressAsynchronously(@NotNull Task.Backgroundable task, @NotNull ProgressIndicator progressIndicator) {
    runProcessWithProgressAsynchronously(task, progressIndicator, null);
  }

  @Override
  public ProgressIndicator getProgressIndicator() {
    return getCurrentIndicator(Thread.currentThread());
  }

  // run in current thread
  @Override
  public void executeProcessUnderProgress(@NotNull Runnable process, ProgressIndicator progress) throws ProcessCanceledException {
    computeUnderProgress(() -> {
      process.run();
      return null;
    }, progress);
  }

  @Override
  public boolean runInReadActionWithWriteActionPriority(@NotNull Runnable action, @Nullable ProgressIndicator indicator) {
    ApplicationManager.getApplication().runReadAction(action);
    return true;
  }

  private <V, E extends Throwable> V computeUnderProgress(@NotNull ThrowableComputable<V, E> process, ProgressIndicator progress) throws E {
    if (progress == null) {
      myUnsafeProgressCount.incrementAndGet();
      try {
        return process.compute();
      }
      finally {
        myUnsafeProgressCount.decrementAndGet();
      }
    }

    ProgressIndicator oldIndicator = getProgressIndicator();
    if (progress == oldIndicator) {
      return process.compute();
    }

    Thread currentThread = Thread.currentThread();
    long threadId = currentThread.getId();
    setCurrentIndicator(threadId, progress);
    try {
      return registerIndicatorAndRun(progress, currentThread, oldIndicator, process);
    }
    finally {
      setCurrentIndicator(threadId, oldIndicator);
    }
  }

  // this thread
  private <V, E extends Throwable> V registerIndicatorAndRun(@NotNull ProgressIndicator indicator,
                                                             @NotNull Thread currentThread,
                                                             ProgressIndicator oldIndicator,
                                                             @NotNull ThrowableComputable<V, E> process) throws E {
    List<Set<Thread>> threadsUnderThisIndicator = new ArrayList<>();
    synchronized (threadsUnderIndicator) {
      boolean oneOfTheIndicatorsIsCanceled = false;

      for (ProgressIndicator thisIndicator = indicator;
           thisIndicator != null;
           thisIndicator = thisIndicator instanceof WrappedProgressIndicator
                           ? ((WrappedProgressIndicator)thisIndicator).getOriginalProgressIndicator()
                           : null) {
        Set<Thread> underIndicator = threadsUnderIndicator.computeIfAbsent(thisIndicator, __ -> new HashSet<>());
        boolean alreadyUnder = !underIndicator.add(currentThread);
        threadsUnderThisIndicator.add(alreadyUnder ? null : underIndicator);

        boolean isStandard = thisIndicator instanceof StandardProgressIndicator;
        if (!isStandard) {
          nonStandardIndicators.compute(thisIndicator, (__, count) -> {
            if (count == null) {
              return new AtomicInteger(1);
            }
            count.incrementAndGet();
            return count;
          });
          startBackgroundNonStandardIndicatorsPing();
        }

        oneOfTheIndicatorsIsCanceled = oneOfTheIndicatorsIsCanceled || thisIndicator.isCanceled();
      }

      updateThreadUnderCanceledIndicator(currentThread, oneOfTheIndicatorsIsCanceled);
    }

    try {
      return process.compute();
    }
    finally {
      synchronized (threadsUnderIndicator) {
        ProgressIndicator thisIndicator = null;
        // order doesn't matter
        for (int i = 0; i < threadsUnderThisIndicator.size(); i++) {
          thisIndicator = i == 0 ? indicator : ((WrappedProgressIndicator)thisIndicator).getOriginalProgressIndicator();
          Set<Thread> underIndicator = threadsUnderThisIndicator.get(i);
          boolean removed = underIndicator != null && underIndicator.remove(currentThread);
          if (removed && underIndicator.isEmpty()) {
            threadsUnderIndicator.remove(thisIndicator);
          }
          boolean isStandard = thisIndicator instanceof StandardProgressIndicator;
          if (!isStandard) {
            AtomicInteger newCount = nonStandardIndicators.compute(thisIndicator, (__, count) -> {
              if (count == null || count.decrementAndGet() == 0) {
                return null;
              }
              return count;
            });
            if (newCount == null) {
              stopBackgroundNonStandardIndicatorsPing();
            }
          }
          // by this time oldIndicator may have been canceled
        }
        updateThreadUnderCanceledIndicator(currentThread, oldIndicator != null && oldIndicator.isCanceled());
      }
    }
  }

  private void updateThreadUnderCanceledIndicator(@NotNull Thread thread, boolean underCanceledIndicator) {
    boolean changed = underCanceledIndicator ? threadsUnderCanceledIndicator.add(thread) : threadsUnderCanceledIndicator.remove(thread);
    if (changed) {
      updateShouldCheckCanceled();
    }
  }

  @ApiStatus.Internal
  public final void updateShouldCheckCanceled() {
    synchronized (threadsUnderIndicator) {
      boolean hasCanceledIndicator = !threadsUnderCanceledIndicator.isEmpty();
      ourCheckCanceledBehavior = !hasCheckCanceledHooks() && !hasCanceledIndicator ? CheckCanceledBehavior.NONE :
                                 hasCanceledIndicator ? CheckCanceledBehavior.INDICATOR_PLUS_HOOKS :
                                 CheckCanceledBehavior.ONLY_HOOKS;
    }
  }

  @Override
  protected void indicatorCanceled(@NotNull ProgressIndicator indicator) {
    // mark threads running under this indicator as canceled
    synchronized (threadsUnderIndicator) {
      Set<Thread> threads = threadsUnderIndicator.get(indicator);
      if (threads != null) {
        for (Thread thread : threads) {
          boolean underCancelledIndicator = false;
          for (ProgressIndicator currentIndicator = getCurrentIndicator(thread);
               currentIndicator != null;
               currentIndicator = currentIndicator instanceof WrappedProgressIndicator ?
                                  ((WrappedProgressIndicator)currentIndicator).getOriginalProgressIndicator() : null) {
            if (currentIndicator == indicator) {
              underCancelledIndicator = true;
              break;
            }
          }

          if (underCancelledIndicator) {
            threadsUnderCanceledIndicator.add(thread);
            updateShouldCheckCanceled();
          }
        }
      }
    }
  }

  @TestOnly
  public static boolean isCanceledThread(@NotNull Thread thread) {
    synchronized (threadsUnderIndicator) {
      return threadsUnderCanceledIndicator.contains(thread);
    }
  }

  @Override
  public boolean isInNonCancelableSection() {
    return Cancellation.isInNonCancelableSection();
  }

  private static final long MAX_PRIORITIZATION_NANOS = TimeUnit.SECONDS.toNanos(12); // maximum duration of process to run under low priority
  private static final long MIN_PRIORITIZATION_NANOS = TimeUnit.MILLISECONDS.toNanos(5); // minimum duration of process to consider prioritizing it down
  private final Set<Thread> myPrioritizedThreads = ConcurrentHashMap.newKeySet();
  private final AtomicInteger myDeprioritizations = new AtomicInteger();
  private volatile long myPrioritizingStartedNanos;

  @Override
  public <T, E extends Throwable> T computePrioritized(@NotNull ThrowableComputable<T, E> computable) throws E {
    Thread thread = Thread.currentThread();
    boolean prioritize;
    if (isCurrentThreadPrioritized()) {
      prioritize = false;
    }
    else {
      prioritize = true;
      if (myPrioritizedThreads.isEmpty()) {
        myPrioritizingStartedNanos = System.nanoTime();
      }
      changePrioritizing(()->myPrioritizedThreads.add(thread));
    }
    try {
      return computable.compute();
    }
    finally {
      if (prioritize) {
        changePrioritizing(()->myPrioritizedThreads.remove(thread));
      }
    }
  }

  private <T> T changePrioritizing(@NotNull Computable<? extends T> runnable) {
    boolean prevIsEmpty = myDeprioritizations.get() > 0 || myPrioritizedThreads.isEmpty();
    T result = runnable.compute();
    boolean currentIsEmpty = myDeprioritizations.get() > 0 || myPrioritizedThreads.isEmpty();
    if (prevIsEmpty && !currentIsEmpty) {
      prioritizingStarted();
    }
    else if (!prevIsEmpty && currentIsEmpty) {
      prioritizingFinished();
    }
    return result;
  }

  protected void prioritizingStarted() {}

  protected void prioritizingFinished() {}

  @ApiStatus.Internal
  public boolean isCurrentThreadPrioritized() {
    return myPrioritizedThreads.contains(Thread.currentThread());
  }

  @ApiStatus.Internal
  public void suppressPrioritizing() {
    int newDeprioritizations = changePrioritizing(()->myDeprioritizations.incrementAndGet());
    if (newDeprioritizations == 100 + ForkJoinPool.getCommonPoolParallelism() * 2) {
      Attachment attachment = new Attachment("threadDump.txt", ThreadDumper.dumpThreadsToString());
      attachment.setIncluded(true);
      LOG.error("A suspiciously high nesting of suppressPrioritizing, forgot to call restorePrioritizing?", attachment);
    }
  }

  @ApiStatus.Internal
  public void restorePrioritizing() {
    int newDeprioritizations = changePrioritizing(()->myDeprioritizations.decrementAndGet());
    if (newDeprioritizations < 0) {
      changePrioritizing(()->myDeprioritizations.getAndSet(0));
      LOG.error("Unmatched suppressPrioritizing/restorePrioritizing");
    }
  }

  protected boolean sleepIfNeededToGivePriorityToAnotherThread() {
    if (isDeprioritizationEnabled() && !isCurrentThreadEffectivelyPrioritized() && isLowPriorityReallyApplicable()) {
      LockSupport.parkNanos(1_000_000);
      avoidBlockingPrioritizingThread();
      return true;
    }
    return false;
  }

  private boolean isCurrentThreadEffectivelyPrioritized() {
    if (myDeprioritizations.get()>0) {
      return false;
    }
    return isCurrentThreadPrioritized();
  }

  private boolean isLowPriorityReallyApplicable() {
    long time = System.nanoTime() - myPrioritizingStartedNanos;
    if (time < MIN_PRIORITIZATION_NANOS) {
      return false; // don't sleep when activities are very short (e.g., empty processing of mouseMoved events)
    }

    if (avoidBlockingPrioritizingThread()) {
      return false;
    }

    if (EDT.isCurrentThreadEdt()) {
      // EDT always has high priority
      return false;
    }

    if (time > MAX_PRIORITIZATION_NANOS) {
      // Don't wait forever in case someone forgot to stop prioritizing before waiting for other threads to complete
      // wait just for 12 seconds; this will be noticeable (and we'll get 2 thread dumps) but not fatal
      stopAllPrioritization();
      return false;
    }
    return true;
  }

  private boolean avoidBlockingPrioritizingThread() {
    if (isAnyPrioritizedThreadBlocked()) {
      // the current thread could hold a lock that prioritized threads are waiting for
      suppressPrioritizing();
      checkLaterThreadsAreUnblocked();
      return true;
    }
    return false;
  }

  private void checkLaterThreadsAreUnblocked() {
    try {
      AppExecutorUtil.getAppScheduledExecutorService().schedule((ContextAwareRunnable)() -> {
        if (isAnyPrioritizedThreadBlocked()) {
          checkLaterThreadsAreUnblocked();
        }
        else {
          restorePrioritizing();
        }
      }, 5, TimeUnit.MILLISECONDS);
    }
    catch (RejectedExecutionException ignore) {
    }
  }

  private void stopAllPrioritization() {
    changePrioritizing(()->{myPrioritizedThreads.clear(); return null;});
  }

  private boolean isAnyPrioritizedThreadBlocked() {
    if (myDeprioritizations.get()>0) {
      return false;
    }
    for (Thread thread : myPrioritizedThreads) {
      Thread.State state = thread.getState();
      if (state == Thread.State.WAITING || state == Thread.State.TIMED_WAITING || state == Thread.State.BLOCKED) {
        return true;
      }
    }
    return false;
  }

  private boolean isDeprioritizationEnabled() {
    return myDeprioritizations.get() < 1_000_000;
  }
  @TestOnly
  @ApiStatus.Internal
  public <T,E extends Throwable> T suppressAllDeprioritizationsDuringLongTestsExecutionIn(@NotNull ThrowableComputable<T, E> runnable) throws E {
    myDeprioritizations.addAndGet(1_000_000);
    try {
      return runnable.compute();
    }
    finally {
      myDeprioritizations.addAndGet(-1_000_000);
    }
  }

  /**
   * @deprecated This method incorrectly prefers {@link com.intellij.openapi.application.ModalityKt#currentThreadContextModality}.
   * Use {@link ModalityState#defaultModalityState()}.
   */
  @Deprecated
  public static @NotNull ModalityState getCurrentThreadProgressModality() {
    ModalityState contextModality = currentThreadContextModality();
    if (contextModality != null) {
      return contextModality;
    }

    ProgressManager progressManager = ProgressManager.getInstanceOrNull();
    ModalityState progressModality = progressManager == null ? null : progressManager.getCurrentProgressModality();
    return progressModality == null ? ModalityState.nonModal() : progressModality;
  }

  private static void setCurrentIndicator(long threadId, ProgressIndicator indicator) {
    if (indicator == null) {
      currentIndicators.remove(threadId);
      threadTopLevelIndicators.remove(threadId);
    }
    else {
      currentIndicators.put(threadId, indicator);
      threadTopLevelIndicators.putIfAbsent(threadId, indicator);
    }
  }

  private static ProgressIndicator getCurrentIndicator(@NotNull Thread thread) {
    return currentIndicators.get(thread.getId());
  }

  @Override
  public <X> X silenceGlobalIndicator(@NotNull Supplier<? extends X> computable) {
    long id = Thread.currentThread().getId();
    ProgressIndicator topLevelIndicator = threadTopLevelIndicators.remove(id);
    ProgressIndicator currentIndicator = currentIndicators.remove(id);
    try {
      return computable.get();
    }
    finally {
      if (currentIndicator != null) {
        currentIndicators.put(id, currentIndicator);
      }
      if (topLevelIndicator != null) {
        threadTopLevelIndicators.put(id, topLevelIndicator);
      }
    }
  }

  @Override
  public @Nullable ModalityState getCurrentProgressModality() {
    ProgressIndicator indicator = threadTopLevelIndicators.get(Thread.currentThread().getId());
    return indicator == null ? null : indicator.getModalityState();
  }

  @FunctionalInterface
  @ApiStatus.Internal
  public interface CheckCanceledHook {
    CheckCanceledHook[] EMPTY_ARRAY = new CheckCanceledHook[0];
    /**
     * @param indicator the indicator whose {@link ProgressIndicator#checkCanceled()} was called,
     *                  or null if {@link ProgressManager#checkCanceled()} was called (even on a thread with indicator)
     * @return true if the hook has done anything that might take some time.
     */
    boolean runHook(@Nullable ProgressIndicator indicator);
  }

  public static void assertUnderProgress(@NotNull ProgressIndicator indicator) {
    synchronized (threadsUnderIndicator) {
      Set<Thread> threads = threadsUnderIndicator.get(indicator);
      if (threads == null || !threads.contains(Thread.currentThread())) {
        ProgressIndicator current = threadTopLevelIndicators.get(Thread.currentThread().getId());
        LOG.error("Must be executed under progress indicator: " + indicator + " but the process is running under "+current+" indicator instead. Please see e.g. ProgressManager.runProcess()");
      }
    }
  }

  @TestOnly
  @ApiStatus.Internal
  public static void __testWhileAlwaysCheckingCanceled(@NotNull Runnable runnable) {
    @SuppressWarnings("InstantiatingAThreadWithDefaultRunMethod")
    Thread fake = new Thread("fake");
    try {
      threadsUnderCanceledIndicator.add(fake);
      runnable.run();
    }
    finally {
      threadsUnderCanceledIndicator.remove(fake);
    }
  }
}
