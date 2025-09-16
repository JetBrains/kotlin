/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package com.intellij.openapi.progress.impl;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.WriteIntentReadAction;
import com.intellij.openapi.application.ex.ApplicationManagerEx;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.openapi.wm.ex.ProgressIndicatorEx;
import com.intellij.util.ExceptionUtil;
import com.intellij.util.Java11Shim;
import com.intellij.util.SystemProperties;
import com.intellij.util.containers.ConcurrentLongObjectMap;
import com.intellij.util.ui.EDT;
import org.jetbrains.annotations.*;

import javax.swing.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

@SuppressWarnings({"UnstableApiUsage", "deprecation"})
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
    if (isInNonCancelableSection()) {
      CheckCanceledBehavior behavior = ourCheckCanceledBehavior;
      if (behavior != CheckCanceledBehavior.NONE) {
        ProgressIndicator indicator = behavior == CheckCanceledBehavior.INDICATOR_PLUS_HOOKS ? getProgressIndicator() : null;
        runCheckCanceledHooks(indicator);
      }
      return;
    }

    Cancellation.ensureActive();

    CheckCanceledBehavior behavior = ourCheckCanceledBehavior;
    if (behavior == CheckCanceledBehavior.NONE) return;

    if (behavior == CheckCanceledBehavior.INDICATOR_PLUS_HOOKS) {
      ProgressIndicator progress = getProgressIndicator();
      if (progress != null) {
        progress.checkCanceled();
      }
    }
    else {
      runCheckCanceledHooks(null);
    }
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
    return "";
  }

  private static void logProcessIndicator(@Nullable ProgressIndicator progress, Boolean started) {
    String progressText = getProgressIndicatorText(progress);
    if (progressText == null) return;
    if (ApplicationManagerEx.isInIntegrationTest()) {
      LOG.info("Progress indicator:" + (started ? "started" : "finished") + ":" + progressText);
    }
  }

  private static void assertNoOtherThreadUnder(@NotNull ProgressIndicator progress) {
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


  // from any: bg, task.finish on "UI/EDT"
  public @NotNull Future<?> runProcessWithProgressAsynchronously(@NotNull Task.Backgroundable task,
                                                        @NotNull ProgressIndicator progressIndicator,
                                                        @Nullable Runnable continuation,
                                                        @NotNull ModalityState modalityState) {
      throw new RuntimeException("Should not be called");
  }

  @ApiStatus.Internal
  public void notifyTaskFinished(@NotNull Task.Backgroundable task, long elapsed) {
  }

  // ASSERT IS EDT->UI bg or calling if can't
  // NEW: no assert; bg or calling ...
  protected boolean runProcessWithProgressSynchronously(@NotNull Task task) {
    if (LOG.isDebugEnabled()) LOG.debug("CoreProgressManager#runProcessWithProgressSynchronously, " + task, new Throwable());
    Ref<Throwable> exceptionRef = new Ref<>();

      try {
          startTask(task, getProgressIndicator(), null);
      }
      catch (ProcessCanceledException e) {
          throw e;
      }
      catch (Throwable e) {
          exceptionRef.set(e);
      }


    finishTask(task, false, exceptionRef.get());

    return false;
  }

  @ApiStatus.Internal
  @VisibleForTesting
  public static boolean shouldEnterModalityState(@NotNull Task task) {
    return task.isModal() ||
           EDT.isCurrentThreadEdt() &&
           !isSynchronousHeadless(task) &&
           isSynchronous(task.asBackgroundable());
  }

  public void runProcessWithProgressInCurrentThread(@NotNull Task task,
                                                    @NotNull ProgressIndicator progressIndicator,
                                                    @NotNull ModalityState modalityState) {
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

    finishTask(task, finalCanceled, finalException);
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
      process.run();
  }

  @Override
  public boolean runInReadActionWithWriteActionPriority(@NotNull Runnable action, @Nullable ProgressIndicator indicator) {
    action.run();
    return true;
  }

  @ApiStatus.Internal
  public final void updateShouldCheckCanceled() {
  }

  @Override
  protected void indicatorCanceled(@NotNull ProgressIndicator indicator) {
    // mark threads running under this indicator as canceled
  }

  @TestOnly
  public static boolean isCanceledThread(@NotNull Thread thread) {
      return false;
  }

  @Override
  public boolean isInNonCancelableSection() {
    return true;
  }

  @Override
  public <T, E extends Throwable> T computePrioritized(@NotNull ThrowableComputable<T, E> computable) throws E {
      return computable.compute();
  }

  protected void prioritizingStarted() {}

  protected void prioritizingFinished() {}

  @ApiStatus.Internal
  public boolean isCurrentThreadPrioritized() {
    return true;
  }

  @ApiStatus.Internal
  public void suppressPrioritizing() {
  }

  @ApiStatus.Internal
  public void restorePrioritizing() {
  }

  protected boolean sleepIfNeededToGivePriorityToAnotherThread() {
    return false;
  }


  @TestOnly
  @ApiStatus.Internal
  public <T,E extends Throwable> T suppressAllDeprioritizationsDuringLongTestsExecutionIn(@NotNull ThrowableComputable<T, E> runnable) throws E {
      return runnable.compute();
  }

  /**
   * @deprecated This method incorrectly prefers {@link com.intellij.openapi.application.ModalityKt#currentThreadContextModality}.
   * Use {@link ModalityState#defaultModalityState()}.
   */
  @Deprecated
  public static @NotNull ModalityState getCurrentThreadProgressModality() {
    return ModalityState.nonModal();
  }


  private static ProgressIndicator getCurrentIndicator(@NotNull Thread thread) {
    return new EmptyProgressIndicator();
  }

  @Override
  public <X> X silenceGlobalIndicator(@NotNull Supplier<? extends X> computable) {
      return computable.get();
  }

  @Override
  public @Nullable ModalityState getCurrentProgressModality() {
      return null;
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
  }

  @TestOnly
  @ApiStatus.Internal
  public static void __testWhileAlwaysCheckingCanceled(@NotNull Runnable runnable) {
      runnable.run();
  }
}
