/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.test.testFramework;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.execution.util.ExecUtil;
import com.intellij.ide.DataManager;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.ide.util.treeView.AbstractTreeStructure;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.extensions.ExtensionPoint;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.extensions.ExtensionsArea;
import com.intellij.openapi.ui.Queryable;
import com.intellij.openapi.util.*;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileFilter;
import com.intellij.util.Alarm;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ReflectionUtil;
import com.intellij.util.ThrowableRunnable;
import com.intellij.util.ui.UIUtil;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jetbrains.annotations.*;
import org.junit.Assert;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.io.*;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("ALL")
public class KtPlatformTestUtil {
    public static final boolean COVERAGE_ENABLED_BUILD = "true".equals(System.getProperty("idea.coverage.enabled.build"));

    private static final boolean SKIP_HEADLESS = GraphicsEnvironment.isHeadless();
    private static final boolean SKIP_SLOW = Boolean.getBoolean("skip.slow.tests.locally");

    @NotNull
    public static String getTestName(@NotNull String name, boolean lowercaseFirstLetter) {
        name = StringUtil.trimStart(name, "test");
        return StringUtil.isEmpty(name) ? "" : lowercaseFirstLetter(name, lowercaseFirstLetter);
    }

    @NotNull
    public static String lowercaseFirstLetter(@NotNull String name, boolean lowercaseFirstLetter) {
        if (lowercaseFirstLetter && !isAllUppercaseName(name)) {
            name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
        }
        return name;
    }

    public static boolean isAllUppercaseName(@NotNull String name) {
        int uppercaseChars = 0;
        for (int i = 0; i < name.length(); i++) {
            if (Character.isLowerCase(name.charAt(i))) {
                return false;
            }
            if (Character.isUpperCase(name.charAt(i))) {
                uppercaseChars++;
            }
        }
        return uppercaseChars >= 3;
    }

    public static <T> void registerExtension(@NotNull ExtensionPointName<T> name, @NotNull T t, @NotNull Disposable parentDisposable) {
        registerExtension(Extensions.getRootArea(), name, t, parentDisposable);
    }

    public static <T> void registerExtension(@NotNull ExtensionsArea area, @NotNull ExtensionPointName<T> name, @NotNull final T t, @NotNull Disposable parentDisposable) {
        final ExtensionPoint<T> extensionPoint = area.getExtensionPoint(name.getName());
        extensionPoint.registerExtension(t);
        Disposer.register(parentDisposable, new Disposable() {
            @Override
            public void dispose() {
                extensionPoint.unregisterExtension(t);
            }
        });
    }

    @Nullable
    protected static String toString(@Nullable Object node, @Nullable Queryable.PrintInfo printInfo) {
        if (node instanceof AbstractTreeNode) {
            if (printInfo != null) {
                return ((AbstractTreeNode)node).toTestString(printInfo);
            }
            else {
                @SuppressWarnings({"deprecation", "UnnecessaryLocalVariable"})
                final String presentation = ((AbstractTreeNode)node).getTestPresentation();
                return presentation;
            }
        }
        if (node == null) {
            return "NULL";
        }
        return node.toString();
    }

    public static String print(JTree tree, boolean withSelection) {
        return print(tree, tree.getModel().getRoot(), withSelection, null, null);
    }

    public static String print(JTree tree, Object root, @Nullable Queryable.PrintInfo printInfo, boolean withSelection) {
        return print(tree, root,  withSelection, printInfo, null);
    }

    public static String print(JTree tree, boolean withSelection, @Nullable Condition<String> nodePrintCondition) {
        return print(tree, tree.getModel().getRoot(), withSelection, null, nodePrintCondition);
    }

    public static String print(JTree tree, Object root,
            boolean withSelection,
            @Nullable Queryable.PrintInfo printInfo,
            @Nullable Condition<String> nodePrintCondition) {
        StringBuilder buffer = new StringBuilder();
        final Collection<String> strings = printAsList(tree, root, withSelection, printInfo, nodePrintCondition);
        for (String string : strings) {
            buffer.append(string).append("\n");
        }
        return buffer.toString();
    }

    public static Collection<String> printAsList(JTree tree, boolean withSelection, @Nullable Condition<String> nodePrintCondition) {
        return printAsList(tree, tree.getModel().getRoot(), withSelection, null, nodePrintCondition);
    }

    private static Collection<String> printAsList(JTree tree, Object root,
            boolean withSelection,
            @Nullable Queryable.PrintInfo printInfo,
            Condition<String> nodePrintCondition) {
        Collection<String> strings = new ArrayList<String>();
        printImpl(tree, root, strings, 0, withSelection, printInfo, nodePrintCondition);
        return strings;
    }

    private static void printImpl(JTree tree,
            Object root,
            Collection<String> strings,
            int level,
            boolean withSelection,
            @Nullable Queryable.PrintInfo printInfo,
            @Nullable Condition<String> nodePrintCondition) {
        DefaultMutableTreeNode defaultMutableTreeNode = (DefaultMutableTreeNode)root;

        final Object userObject = defaultMutableTreeNode.getUserObject();
        String nodeText;
        if (userObject != null) {
            nodeText = toString(userObject, printInfo);
        }
        else {
            nodeText = "null";
        }

        if (nodePrintCondition != null && !nodePrintCondition.value(nodeText)) return;

        final StringBuilder buff = new StringBuilder();
        StringUtil.repeatSymbol(buff, ' ', level);

        final boolean expanded = tree.isExpanded(new TreePath(defaultMutableTreeNode.getPath()));
        if (!defaultMutableTreeNode.isLeaf()) {
            buff.append(expanded ? "-" : "+");
        }

        final boolean selected = tree.getSelectionModel().isPathSelected(new TreePath(defaultMutableTreeNode.getPath()));
        if (withSelection && selected) {
            buff.append("[");
        }

        buff.append(nodeText);

        if (withSelection && selected) {
            buff.append("]");
        }

        strings.add(buff.toString());

        int childCount = tree.getModel().getChildCount(root);
        if (expanded) {
            for (int i = 0; i < childCount; i++) {
                printImpl(tree, tree.getModel().getChild(root, i), strings, level + 1, withSelection, printInfo, nodePrintCondition);
            }
        }
    }

    public static void assertTreeEqual(JTree tree, @NonNls String expected) {
        assertTreeEqual(tree, expected, false);
    }

    public static void assertTreeEqualIgnoringNodesOrder(JTree tree, @NonNls String expected) {
        assertTreeEqualIgnoringNodesOrder(tree, expected, false);
    }

    public static void assertTreeEqual(JTree tree, String expected, boolean checkSelected) {
        String treeStringPresentation = print(tree, checkSelected);
        Assert.assertEquals(expected, treeStringPresentation);
    }

    public static void assertTreeEqualIgnoringNodesOrder(JTree tree, String expected, boolean checkSelected) {
        final Collection<String> actualNodesPresentation = printAsList(tree, checkSelected, null);
        final java.util.List<String> expectedNodes = StringUtil.split(expected, "\n");
        KtUsefulTestCase.assertSameElements(actualNodesPresentation, expectedNodes);
    }

    @TestOnly
    public static void waitForAlarm(final int delay) throws InterruptedException {
        assert !ApplicationManager.getApplication().isWriteAccessAllowed(): "It's a bad idea to wait for an alarm under the write action. Somebody creates an alarm which requires read action and you are deadlocked.";
        assert ApplicationManager.getApplication().isDispatchThread();

        final AtomicBoolean invoked = new AtomicBoolean();
        final Alarm alarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD);
        alarm.addRequest(new Runnable() {
            @Override
            public void run() {
                ApplicationManager.getApplication().invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        alarm.addRequest(new Runnable() {
                            @Override
                            public void run() {
                                invoked.set(true);
                            }
                        }, delay);
                    }
                });
            }
        }, delay);

        UIUtil.dispatchAllInvocationEvents();

        boolean sleptAlready = false;
        while (!invoked.get()) {
            UIUtil.dispatchAllInvocationEvents();
            //noinspection BusyWait
            Thread.sleep(sleptAlready ? 10 : delay);
            sleptAlready = true;
        }
        UIUtil.dispatchAllInvocationEvents();
    }

    //@TestOnly
    //public static void dispatchAllInvocationEventsInIdeEventQueue() throws InterruptedException {
    //    assert SwingUtilities.isEventDispatchThread() : Thread.currentThread();
    //    final EventQueue eventQueue = Toolkit.getDefaultToolkit().getSystemEventQueue();
    //    while (true) {
    //        AWTEvent event = eventQueue.peekEvent();
    //        if (event == null) break;
    //        AWTEvent event1 = eventQueue.getNextEvent();
    //        if (event1 instanceof InvocationEvent) {
    //            IdeEventQueue.getInstance().dispatchEvent(event1);
    //        }
    //    }
    //}

    //private static Date raidDate(Bombed bombed) {
    //    final Calendar instance = Calendar.getInstance();
    //    instance.set(Calendar.YEAR, bombed.year());
    //    instance.set(Calendar.MONTH, bombed.month());
    //    instance.set(Calendar.DAY_OF_MONTH, bombed.day());
    //    instance.set(Calendar.HOUR_OF_DAY, bombed.time());
    //    instance.set(Calendar.MINUTE, 0);
    //
    //    return instance.getTime();
    //}
    //
    //public static boolean bombExplodes(Bombed bombedAnnotation) {
    //    Date now = new Date();
    //    return now.after(raidDate(bombedAnnotation));
    //}

    public static StringBuilder print(AbstractTreeStructure structure,
            Object node,
            int currentLevel,
            @Nullable Comparator comparator,
            int maxRowCount,
            char paddingChar,
            @Nullable Queryable.PrintInfo printInfo) {
        StringBuilder buffer = new StringBuilder();
        doPrint(buffer, currentLevel, node, structure, comparator, maxRowCount, 0, paddingChar, printInfo);
        return buffer;
    }

    private static int doPrint(StringBuilder buffer,
            int currentLevel,
            Object node,
            AbstractTreeStructure structure,
            @Nullable Comparator comparator,
            int maxRowCount,
            int currentLine,
            char paddingChar,
            @Nullable Queryable.PrintInfo printInfo) {
        if (currentLine >= maxRowCount && maxRowCount != -1) return currentLine;

        StringUtil.repeatSymbol(buffer, paddingChar, currentLevel);
        buffer.append(toString(node, printInfo)).append("\n");
        currentLine++;
        Object[] children = structure.getChildElements(node);

        if (comparator != null) {
            ArrayList<?> list = new ArrayList<Object>(Arrays.asList(children));
            @SuppressWarnings({"UnnecessaryLocalVariable", "unchecked"}) Comparator<Object> c = comparator;
            Collections.sort(list, c);
            children = ArrayUtil.toObjectArray(list);
        }
        for (Object child : children) {
            currentLine = doPrint(buffer, currentLevel + 1, child, structure, comparator, maxRowCount, currentLine, paddingChar, printInfo);
        }

        return currentLine;
    }

    public static String print(Object[] objects) {
        return print(Arrays.asList(objects));
    }

    public static String print(Collection c) {
        StringBuilder result = new StringBuilder();
        for (Iterator iterator = c.iterator(); iterator.hasNext();) {
            Object each = iterator.next();
            result.append(toString(each, null));
            if (iterator.hasNext()) {
                result.append("\n");
            }
        }

        return result.toString();
    }

    public static String print(ListModel model) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < model.getSize(); i++) {
            result.append(toString(model.getElementAt(i), null));
            result.append("\n");
        }
        return result.toString();
    }

    public static String print(JTree tree) {
        return print(tree, false);
    }

    public static void assertTreeStructureEquals(final AbstractTreeStructure treeStructure, final String expected) {
        Assert.assertEquals(expected, print(treeStructure, treeStructure.getRootElement(), 0, null, -1, ' ', null).toString());
    }

    public static void invokeNamedAction(final String actionId) {
        final AnAction action = ActionManager.getInstance().getAction(actionId);
        Assert.assertNotNull(action);
        final Presentation presentation = new Presentation();
        @SuppressWarnings("deprecation") final DataContext context = DataManager.getInstance().getDataContext();
        final AnActionEvent event = AnActionEvent.createFromAnAction(action, null, "", context);
        action.update(event);
        Assert.assertTrue(presentation.isEnabled());
        action.actionPerformed(event);
    }

    //public static void assertTiming(final String message, final long expectedMs, final long actual) {
    //    if (COVERAGE_ENABLED_BUILD) return;
    //
    //    final long expectedOnMyMachine = Math.max(1, expectedMs * Timings.MACHINE_TIMING / Timings.ETALON_TIMING);
    //
    //    // Allow 10% more in case of test machine is busy.
    //    String logMessage = message;
    //    if (actual > expectedOnMyMachine) {
    //        int percentage = (int)(100.0 * (actual - expectedOnMyMachine) / expectedOnMyMachine);
    //        logMessage += ". Operation took " + percentage + "% longer than expected";
    //    }
    //    logMessage += ". Expected on my machine: " + expectedOnMyMachine + "." +
    //                  " Actual: " + actual + "." +
    //                  " Expected on Standard machine: " + expectedMs + ";" +
    //                  " Actual on Standard: " + actual * Timings.ETALON_TIMING / Timings.MACHINE_TIMING + ";" +
    //                  " Timings: CPU=" + Timings.CPU_TIMING +
    //                  ", I/O=" + Timings.IO_TIMING + "." +
    //                  " (" + (int)(Timings.MACHINE_TIMING*1.0/Timings.ETALON_TIMING*100) + "% of the Standard)" +
    //                  ".";
    //    final double acceptableChangeFactor = 1.1;
    //    if (actual < expectedOnMyMachine) {
    //        System.out.println(logMessage);
    //        TeamCityLogger.info(logMessage);
    //    }
    //    else if (actual < expectedOnMyMachine * acceptableChangeFactor) {
    //        TeamCityLogger.warning(logMessage, null);
    //    }
    //    else {
    //        // throw AssertionFailedError to try one more time
    //        throw new AssertionFailedError(logMessage);
    //    }
    //}

    /**
     * example usage: startPerformanceTest("calculating pi",100, testRunnable).cpuBound().assertTiming();
     */
    @Contract(pure = true) // to warn about not calling .assertTiming() in the end
    public static TestInfo startPerformanceTest(@NonNls @NotNull String message, int expectedMs, @NotNull ThrowableRunnable test) {
        return new TestInfo(test, expectedMs,message);
    }

    public static boolean canRunTest(@NotNull Class testCaseClass) {
        if (!SKIP_SLOW && !SKIP_HEADLESS) {
            return true;
        }

        for (Class<?> clazz = testCaseClass; clazz != null; clazz = clazz.getSuperclass()) {
            //if (SKIP_HEADLESS && clazz.getAnnotation(SkipInHeadlessEnvironment.class) != null) {
            //    System.out.println("Class '" + testCaseClass.getName() + "' is skipped because it requires working UI environment");
            //    return false;
            //}
            //if (SKIP_SLOW && clazz.getAnnotation(SkipSlowTestLocally.class) != null) {
            //    System.out.println("Class '" + testCaseClass.getName() + "' is skipped because it is dog slow");
            //    return false;
            //}
        }

        return true;
    }

    public static void assertPathsEqual(@Nullable String expected, @Nullable String actual) {
        if (expected != null) expected = FileUtil.toSystemIndependentName(expected);
        if (actual != null) actual = FileUtil.toSystemIndependentName(actual);
        Assert.assertEquals(expected, actual);
    }

    @NotNull
    public static String getRtJarPath() {
        String home = System.getProperty("java.home");
        return SystemInfo.isAppleJvm ? FileUtil.toCanonicalPath(home + "/../Classes/classes.jar") : home + "/lib/rt.jar";
    }

    //public static void saveProject(Project project) {
    //    ApplicationEx application = ApplicationManagerEx.getApplicationEx();
    //    boolean oldValue = application.isDoNotSave();
    //    try {
    //        application.doNotSave(false);
    //        project.save();
    //    }
    //    finally {
    //        application.doNotSave(oldValue);
    //    }
    //}

    public static class TestInfo {
        private final ThrowableRunnable test; // runnable to measure
        private final int expectedMs;           // millis the test is expected to run
        private ThrowableRunnable setup;      // to run before each test
        private boolean usesAllCPUCores;      // true if the test runs faster on multi-core
        private int attempts = 4;             // number of retries if performance failed
        private final String message;         // to print on fail
        private boolean adjustForIO = true;   // true if test uses IO, timings need to be re-calibrated according to this agent disk performance
        private boolean adjustForCPU = true;  // true if test uses CPU, timings need to be re-calibrated according to this agent CPU speed
        private boolean useLegacyScaling;

        private TestInfo(@NotNull ThrowableRunnable test, int expectedMs, String message) {
            this.test = test;
            this.expectedMs = expectedMs;
            assert expectedMs > 0 : "Expected must be > 0. Was: "+ expectedMs;
            this.message = message;
        }

        @Contract(pure = true) // to warn about not calling .assertTiming() in the end
        public TestInfo setup(@NotNull ThrowableRunnable setup) { assert this.setup==null; this.setup = setup; return this; }
        @Contract(pure = true) // to warn about not calling .assertTiming() in the end
        public TestInfo usesAllCPUCores() { assert adjustForCPU : "This test configured to be io-bound, it cannot use all cores"; usesAllCPUCores = true; return this; }
        @Contract(pure = true) // to warn about not calling .assertTiming() in the end
        public TestInfo cpuBound() { adjustForIO = false; adjustForCPU = true; return this; }
        @Contract(pure = true) // to warn about not calling .assertTiming() in the end
        public TestInfo ioBound() { adjustForIO = true; adjustForCPU = false; return this; }
        @Contract(pure = true) // to warn about not calling .assertTiming() in the end
        public TestInfo attempts(int attempts) { this.attempts = attempts; return this; }
        /**
         * @deprecated Enables procedure for nonlinear scaling of results between different machines. This was historically enabled, but doesn't
         * seem to be meaningful, and is known to make results worse in some cases. Consider migration off this setting, recalibrating
         * expected execution time accordingly.
         */
        @Contract(pure = true) // to warn about not calling .assertTiming() in the end
        public TestInfo useLegacyScaling() { useLegacyScaling = true; return this; }

        //public void assertTiming() {
        //    assert expectedMs != 0 : "Must call .expect() before run test";
        //    if (COVERAGE_ENABLED_BUILD) return;
        //    Timings.getStatistics(); // warm-up, measure
        //
        //    while (true) {
        //        attempts--;
        //        long start;
        //        try {
        //            if (setup != null) setup.run();
        //            start = System.currentTimeMillis();
        //            test.run();
        //        }
        //        catch (Throwable throwable) {
        //            throw new RuntimeException(throwable);
        //        }
        //        long finish = System.currentTimeMillis();
        //        long duration = finish - start;
        //
        //        int expectedOnMyMachine = expectedMs;
        //        if (adjustForCPU) {
        //            expectedOnMyMachine = adjust(expectedOnMyMachine, Timings.CPU_TIMING, Timings.ETALON_CPU_TIMING, useLegacyScaling);
        //
        //            expectedOnMyMachine = usesAllCPUCores ? expectedOnMyMachine * 8 / JobSchedulerImpl.CORES_COUNT : expectedOnMyMachine;
        //        }
        //        if (adjustForIO) {
        //            expectedOnMyMachine = adjust(expectedOnMyMachine, Timings.IO_TIMING, Timings.ETALON_IO_TIMING, useLegacyScaling);
        //        }
        //
        //        // Allow 10% more in case of test machine is busy.
        //        String logMessage = message;
        //        if (duration > expectedOnMyMachine) {
        //            int percentage = (int)(100.0 * (duration - expectedOnMyMachine) / expectedOnMyMachine);
        //            logMessage += ": " + percentage + "% longer";
        //        }
        //        logMessage +=
        //                ". Expected: " + formatTime(expectedOnMyMachine) + ". Actual: " + formatTime(duration) + "." + Timings.getStatistics();
        //        final double acceptableChangeFactor = 1.1;
        //        if (duration < expectedOnMyMachine) {
        //            int percentage = (int)(100.0 * (expectedOnMyMachine - duration) / expectedOnMyMachine);
        //            logMessage = percentage + "% faster. " + logMessage;
        //
        //            TeamCityLogger.info(logMessage);
        //            System.out.println("SUCCESS: " + logMessage);
        //        }
        //        else if (duration < expectedOnMyMachine * acceptableChangeFactor) {
        //            TeamCityLogger.warning(logMessage, null);
        //            System.out.println("WARNING: " + logMessage);
        //        }
        //        else {
        //            // try one more time
        //            if (attempts == 0) {
        //                //try {
        //                //  Object result = Class.forName("com.intellij.util.ProfilingUtil").getMethod("captureCPUSnapshot").invoke(null);
        //                //  System.err.println("CPU snapshot captured in '"+result+"'");
        //                //}
        //                //catch (Exception e) {
        //                //}
        //
        //                throw new AssertionFailedError(logMessage);
        //            }
        //            System.gc();
        //            System.gc();
        //            System.gc();
        //            String s = "Another epic fail (remaining attempts: " + attempts + "): " + logMessage;
        //            TeamCityLogger.warning(s, null);
        //            System.err.println(s);
        //            //if (attempts == 1) {
        //            //  try {
        //            //    Class.forName("com.intellij.util.ProfilingUtil").getMethod("startCPUProfiling").invoke(null);
        //            //  }
        //            //  catch (Exception e) {
        //            //  }
        //            //}
        //            continue;
        //        }
        //        break;
        //    }
        //}

        private static String formatTime(long millis) {
            String hint = "";
            DecimalFormat format = new DecimalFormat("#.0", DecimalFormatSymbols.getInstance(Locale.US));
            if (millis >= 60 * 1000) hint = format.format(millis / 60 / 1000.f) + "m";
            if (millis >= 1000) hint += (hint.isEmpty() ? "" : " ") + format.format(millis / 1000.f) + "s";
            String result = millis + "ms";
            if (!hint.isEmpty()) {
                result = result + " (" + hint + ")";
            }
            return result;
        }

        private static int adjust(int expectedOnMyMachine, long thisTiming, long etalonTiming, boolean useLegacyScaling) {
            if (useLegacyScaling) {
                double speed = 1.0 * thisTiming / etalonTiming;
                double delta = speed < 1
                               ? 0.9 + Math.pow(speed - 0.7, 2)
                               : 0.45 + Math.pow(speed - 0.25, 2);
                expectedOnMyMachine *= delta;
                return expectedOnMyMachine;
            }
            else {
                return (int)(expectedOnMyMachine * thisTiming / etalonTiming);
            }
        }
    }


    //public static void assertTiming(String message, long expected, @NotNull Runnable actionToMeasure) {
    //    assertTiming(message, expected, 4, actionToMeasure);
    //}

    public static long measure(@NotNull Runnable actionToMeasure) {
        long start = System.currentTimeMillis();
        actionToMeasure.run();
        long finish = System.currentTimeMillis();
        return finish - start;
    }

    //public static void assertTiming(String message, long expected, int attempts, @NotNull Runnable actionToMeasure) {
    //    while (true) {
    //        attempts--;
    //        long duration = measure(actionToMeasure);
    //        try {
    //            assertTiming(message, expected, duration);
    //            break;
    //        }
    //        catch (AssertionFailedError e) {
    //            if (attempts == 0) throw e;
    //            System.gc();
    //            System.gc();
    //            System.gc();
    //            String s = "Another epic fail (remaining attempts: " + attempts + "): " + e.getMessage();
    //            TeamCityLogger.warning(s, null);
    //            System.err.println(s);
    //        }
    //    }
    //}

    private static com.intellij.util.containers.HashMap<String, VirtualFile> buildNameToFileMap(VirtualFile[] files, @Nullable VirtualFileFilter filter) {
        com.intellij.util.containers.HashMap<String, VirtualFile> map = new com.intellij.util.containers.HashMap<String, VirtualFile>();
        for (VirtualFile file : files) {
            if (filter != null && !filter.accept(file)) continue;
            map.put(file.getName(), file);
        }
        return map;
    }

    //public static void assertDirectoriesEqual(VirtualFile dirAfter, VirtualFile dirBefore) throws IOException {
    //    assertDirectoriesEqual(dirAfter, dirBefore, null);
    //}

    //@SuppressWarnings("UnsafeVfsRecursion")
    //public static void assertDirectoriesEqual(VirtualFile dirAfter, VirtualFile dirBefore, @Nullable VirtualFileFilter fileFilter) throws IOException {
    //    FileDocumentManager.getInstance().saveAllDocuments();
    //
    //    VirtualFile[] childrenAfter = dirAfter.getChildren();
    //
    //    if (dirAfter.isInLocalFileSystem() && dirAfter.getFileSystem() != TempFileSystem.getInstance()) {
    //        File[] ioAfter = new File(dirAfter.getPath()).listFiles();
    //        shallowCompare(childrenAfter, ioAfter);
    //    }
    //
    //    VirtualFile[] childrenBefore = dirBefore.getChildren();
    //    if (dirBefore.isInLocalFileSystem() && dirBefore.getFileSystem() != TempFileSystem.getInstance()) {
    //        File[] ioBefore = new File(dirBefore.getPath()).listFiles();
    //        shallowCompare(childrenBefore, ioBefore);
    //    }
    //
    //    com.intellij.util.containers.HashMap<String, VirtualFile> mapAfter = buildNameToFileMap(childrenAfter, fileFilter);
    //    com.intellij.util.containers.HashMap<String, VirtualFile> mapBefore = buildNameToFileMap(childrenBefore, fileFilter);
    //
    //    Set<String> keySetAfter = mapAfter.keySet();
    //    Set<String> keySetBefore = mapBefore.keySet();
    //    Assert.assertEquals(dirAfter.getPath(), keySetAfter, keySetBefore);
    //
    //    for (String name : keySetAfter) {
    //        VirtualFile fileAfter = mapAfter.get(name);
    //        VirtualFile fileBefore = mapBefore.get(name);
    //        if (fileAfter.isDirectory()) {
    //            assertDirectoriesEqual(fileAfter, fileBefore, fileFilter);
    //        }
    //        else {
    //            assertFilesEqual(fileAfter, fileBefore);
    //        }
    //    }
    //}

    private static void shallowCompare(VirtualFile[] vfs, @Nullable File[] io) {
        java.util.List<String> vfsPaths = new ArrayList<String>();
        for (VirtualFile file : vfs) {
            vfsPaths.add(file.getPath());
        }

        java.util.List<String> ioPaths = new ArrayList<String>();
        if (io != null) {
            for (File file : io) {
                ioPaths.add(file.getPath().replace(File.separatorChar, '/'));
            }
        }

        Assert.assertEquals(sortAndJoin(vfsPaths), sortAndJoin(ioPaths));
    }

    private static String sortAndJoin(java.util.List<String> strings) {
        Collections.sort(strings);
        StringBuilder buf = new StringBuilder();
        for (String string : strings) {
            buf.append(string);
            buf.append('\n');
        }
        return buf.toString();
    }

    //public static void assertFilesEqual(VirtualFile fileAfter, VirtualFile fileBefore) throws IOException {
    //    try {
    //        assertJarFilesEqual(VfsUtilCore.virtualToIoFile(fileAfter), VfsUtilCore.virtualToIoFile(fileBefore));
    //    }
    //    catch (IOException e) {
    //        FileDocumentManager manager = FileDocumentManager.getInstance();
    //
    //        Document docBefore = manager.getDocument(fileBefore);
    //        boolean canLoadBeforeText = !fileBefore.getFileType().isBinary() || fileBefore.getFileType() == FileTypes.UNKNOWN;
    //        String textB = docBefore != null
    //                       ? docBefore.getText()
    //                       : !canLoadBeforeText
    //                         ? null
    //                         : LoadTextUtil.getTextByBinaryPresentation(fileBefore.contentsToByteArray(false), fileBefore).toString();
    //
    //        Document docAfter = manager.getDocument(fileAfter);
    //        boolean canLoadAfterText = !fileBefore.getFileType().isBinary() || fileBefore.getFileType() == FileTypes.UNKNOWN;
    //        String textA = docAfter != null
    //                       ? docAfter.getText()
    //                       : !canLoadAfterText
    //                         ? null
    //                         : LoadTextUtil.getTextByBinaryPresentation(fileAfter.contentsToByteArray(false), fileAfter).toString();
    //
    //        if (textA != null && textB != null) {
    //            Assert.assertEquals(fileAfter.getPath(), textA, textB);
    //        }
    //        else {
    //            Assert.assertArrayEquals(fileAfter.getPath(), fileAfter.contentsToByteArray(), fileBefore.contentsToByteArray());
    //        }
    //    }
    //}

    //public static void assertJarFilesEqual(File file1, File file2) throws IOException {
    //    final File tempDirectory1;
    //    final File tempDirectory2;
    //
    //    final JarFile jarFile1 = new JarFile(file1);
    //    try {
    //        final JarFile jarFile2 = new JarFile(file2);
    //        try {
    //            tempDirectory1 = PlatformTestCase.createTempDir("tmp1");
    //            tempDirectory2 = PlatformTestCase.createTempDir("tmp2");
    //            ZipUtil.extract(jarFile1, tempDirectory1, null);
    //            ZipUtil.extract(jarFile2, tempDirectory2, null);
    //        }
    //        finally {
    //            jarFile2.close();
    //        }
    //    }
    //    finally {
    //        jarFile1.close();
    //    }
    //
    //    final VirtualFile dirAfter = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(tempDirectory1);
    //    Assert.assertNotNull(tempDirectory1.toString(), dirAfter);
    //    final VirtualFile dirBefore = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(tempDirectory2);
    //    Assert.assertNotNull(tempDirectory2.toString(), dirBefore);
    //    ApplicationManager.getApplication().runWriteAction(new Runnable() {
    //        @Override
    //        public void run() {
    //            dirAfter.refresh(false, true);
    //            dirBefore.refresh(false, true);
    //        }
    //    });
    //    assertDirectoriesEqual(dirAfter, dirBefore);
    //}

    public static void assertElementsEqual(final Element expected, final Element actual) throws IOException {
        if (!JDOMUtil.areElementsEqual(expected, actual)) {
            Assert.assertEquals(printElement(expected), printElement(actual));
        }
    }

    public static void assertElementEquals(final String expected, final Element actual) {
        try {
            assertElementsEqual(JDOMUtil.loadDocument(expected).getRootElement(), actual);
        }
        catch (IOException e) {
            throw new AssertionError(e);
        }
        catch (JDOMException e) {
            throw new AssertionError(e);
        }
    }

    public static String printElement(final Element element) throws IOException {
        final StringWriter writer = new StringWriter();
        JDOMUtil.writeElement(element, writer, "\n");
        return writer.getBuffer().toString();
    }

    public static String getCommunityPath() {
        final String homePath = PathManager.getHomePath();
        if (new File(homePath, "community/.idea").isDirectory()) {
            return homePath + File.separatorChar + "community";
        }
        return homePath;
    }

    public static String getPlatformTestDataPath() {
        return getCommunityPath().replace(File.separatorChar, '/') + "/platform/platform-tests/testData/";
    }


    public static Comparator<AbstractTreeNode> createComparator(final Queryable.PrintInfo printInfo) {
        return new Comparator<AbstractTreeNode>() {
            @Override
            public int compare(final AbstractTreeNode o1, final AbstractTreeNode o2) {
                String displayText1 = o1.toTestString(printInfo);
                String displayText2 = o2.toTestString(printInfo);
                return Comparing.compare(displayText1, displayText2);
            }
        };
    }

    @NotNull
    public static <T> T notNull(@Nullable T t) {
        Assert.assertNotNull(t);
        return t;
    }

    @NotNull
    public static String loadFileText(@NotNull String fileName) throws IOException {
        return StringUtil.convertLineSeparators(FileUtil.loadFile(new File(fileName)));
    }

    //public static void tryGcSoftlyReachableObjects() {
    //    GCUtil.tryGcSoftlyReachableObjects();
    //}

    public static void withEncoding(@NotNull String encoding, @NotNull final Runnable r) {
        withEncoding(encoding, new ThrowableRunnable() {
            @Override
            public void run() throws Throwable {
                r.run();
            }
        });
    }

    public static void withEncoding(@NotNull String encoding, @NotNull ThrowableRunnable r) {
        Charset oldCharset = Charset.defaultCharset();
        try {
            try {
                patchSystemFileEncoding(encoding);
                r.run();
            }
            finally {
                patchSystemFileEncoding(oldCharset.name());
            }
        }
        catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private static void patchSystemFileEncoding(String encoding) {
        ReflectionUtil.resetField(Charset.class, Charset.class, "defaultCharset");
        System.setProperty("file.encoding", encoding);
    }

    public static void withStdErrSuppressed(@NotNull Runnable r) {
        PrintStream std = System.err;
        System.setErr(new PrintStream(NULL));
        try {
            r.run();
        }
        finally {
            System.setErr(std);
        }
    }

    @SuppressWarnings("IOResourceOpenedButNotSafelyClosed")
    private static final OutputStream NULL = new OutputStream() {
        @Override
        public void write(int b) throws IOException { }
    };

    public static void assertSuccessful(@NotNull GeneralCommandLine command) {
        try {
            ProcessOutput output = ExecUtil.execAndGetOutput(command.withRedirectErrorStream(true));
            Assert.assertEquals(output.getStdout(), 0, output.getExitCode());
        }
        catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}