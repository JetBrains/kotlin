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

import com.intellij.diagnostic.PerformanceWatcher;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.application.impl.ApplicationInfoImpl;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.*;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.rt.execution.junit.FileComparisonFailure;
import com.intellij.testFramework.*;
import com.intellij.testFramework.exceptionCases.AbstractExceptionCase;
import com.intellij.util.Consumer;
import com.intellij.util.ReflectionUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.hash.HashMap;
import com.intellij.util.lang.CompoundRuntimeException;
import com.intellij.util.ui.UIUtil;
import gnu.trove.Equality;
import gnu.trove.THashSet;
import junit.framework.AssertionFailedError;
import junit.framework.TestCase;
import org.jdom.Element;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.test.IdeaSystemPropertiesForParallelRunConfigurator;
import org.jetbrains.kotlin.testFramework.MockComponentManagerCreationTracer;
import org.jetbrains.kotlin.types.AbstractTypeChecker;
import org.jetbrains.kotlin.types.FlexibleTypeImpl;
import org.jetbrains.kotlin.utils.ExceptionUtilsKt;
import org.junit.Assert;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

@SuppressWarnings("UseOfSystemOutOrSystemErr")
public abstract class KtUsefulTestCase extends TestCase {
    public static final boolean IS_UNDER_TEAMCITY = System.getenv("TEAMCITY_VERSION") != null;
    private static final String TEMP_DIR_MARKER = "unitTest_";
    public static final boolean OVERWRITE_TESTDATA = Boolean.getBoolean("idea.tests.overwrite.data");

    private static final String ORIGINAL_TEMP_DIR = FileUtil.getTempDirectory();

    private static final Map<String, Long> TOTAL_SETUP_COST_MILLIS = new HashMap<>();
    private static final Map<String, Long> TOTAL_TEARDOWN_COST_MILLIS = new HashMap<>();

    private Application application;

    static {
        IdeaSystemPropertiesForParallelRunConfigurator.setProperties();
        //TODO: investigate and enable
        //IdeaForkJoinWorkerThreadFactory.setupPoisonFactory();
        Logger.setFactory(TestLoggerFactory.class);
    }

    @NotNull
    protected final Disposable myTestRootDisposable = new TestDisposable();

    private static final String ourPathToKeep = null;
    private final List<String> myPathsToKeep = new ArrayList<>();

    private File myTempDir;

    static {
        // Radar #5755208: Command line Java applications need a way to launch without a Dock icon.
        System.setProperty("apple.awt.UIElement", "true");

        FlexibleTypeImpl.RUN_SLOW_ASSERTIONS = true;
        AbstractTypeChecker.RUN_SLOW_ASSERTIONS = true;
    }

    protected boolean shouldContainTempFiles() {
        return true;
    }

    @Override
    protected void setUp() throws Exception {
        application = ApplicationManager.getApplication();

        if (application != null && application.isDisposed()) {
            MockComponentManagerCreationTracer.diagnoseDisposedButNotClearedApplication(application);
        }

        super.setUp();

        if (shouldContainTempFiles()) {
            String testName =  FileUtil.sanitizeFileName(getTestName(true));
            if (StringUtil.isEmptyOrSpaces(testName)) testName = "";
            testName = new File(testName).getName(); // in case the test name contains file separators
            myTempDir = FileUtil.createTempDirectory(TEMP_DIR_MARKER, testName, false);
            FileUtil.resetCanonicalTempPathCache(myTempDir.getPath());
        }
        boolean isStressTest = isStressTest();
        ApplicationInfoImpl.setInStressTest(isStressTest);
        if (isPerformanceTest()) {
            Timings.getStatistics();
        }
        // turn off Disposer debugging for performance tests
        Disposer.setDebugMode(!isStressTest);
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            // don't use method references here to make stack trace reading easier
            //noinspection Convert2MethodRef
            new RunAll(
                    () -> disposeRootDisposable(),
                    () -> cleanupSwingDataStructures(),
                    () -> cleanupDeleteOnExitHookList(),
                    () -> Disposer.setDebugMode(true),
                    () -> {
                        if (shouldContainTempFiles()) {
                            FileUtil.resetCanonicalTempPathCache(ORIGINAL_TEMP_DIR);
                            if (hasTmpFilesToKeep()) {
                                File[] files = myTempDir.listFiles();
                                if (files != null) {
                                    for (File file : files) {
                                        if (!shouldKeepTmpFile(file)) {
                                            FileUtil.delete(file);
                                        }
                                    }
                                }
                            }
                            else {
                                FileUtil.delete(myTempDir);
                            }
                        }
                    },
                    () -> UIUtil.removeLeakingAppleListeners()
            ).run();
        }
        finally {
            super.tearDown();
            resetApplicationToNull(application);
            application = null;
        }
    }

    public static void resetApplicationToNull(Application old) {
        if (old != null) return;
        resetApplicationToNull();
    }

    public static void resetApplicationToNull() {
        try {
            Field ourApplicationField = ApplicationManager.class.getDeclaredField("ourApplication");
            ourApplicationField.setAccessible(true);
            ourApplicationField.set(null, null);
        }
        catch (Exception e) {
            throw ExceptionUtilsKt.rethrow(e);
        }
    }


    protected final void disposeRootDisposable() {
        Disposer.dispose(getTestRootDisposable());
    }

    protected void addTmpFileToKeep(@NotNull File file) {
        myPathsToKeep.add(file.getPath());
    }

    private boolean hasTmpFilesToKeep() {
        return ourPathToKeep != null && FileUtil.isAncestor(myTempDir.getPath(), ourPathToKeep, false) || !myPathsToKeep.isEmpty();
    }

    private boolean shouldKeepTmpFile(@NotNull File file) {
        String path = file.getPath();
        if (FileUtil.pathsEqual(path, ourPathToKeep)) return true;
        for (String pathToKeep : myPathsToKeep) {
            if (FileUtil.pathsEqual(path, pathToKeep)) return true;
        }
        return false;
    }

    private static final Set<String> DELETE_ON_EXIT_HOOK_DOT_FILES;
    private static final Class DELETE_ON_EXIT_HOOK_CLASS;
    static {
        Class<?> aClass;
        try {
            aClass = Class.forName("java.io.DeleteOnExitHook");
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        @SuppressWarnings("unchecked") Set<String> files = ReflectionUtil.getStaticFieldValue(aClass, Set.class, "files");
        DELETE_ON_EXIT_HOOK_CLASS = aClass;
        DELETE_ON_EXIT_HOOK_DOT_FILES = files;
    }

    @SuppressWarnings("SynchronizeOnThis")
    private static void cleanupDeleteOnExitHookList() {
        // try to reduce file set retained by java.io.DeleteOnExitHook
        List<String> list;
        synchronized (DELETE_ON_EXIT_HOOK_CLASS) {
            if (DELETE_ON_EXIT_HOOK_DOT_FILES.isEmpty()) return;
            list = new ArrayList<>(DELETE_ON_EXIT_HOOK_DOT_FILES);
        }
        for (int i = list.size() - 1; i >= 0; i--) {
            String path = list.get(i);
            File file = new File(path);
            if (file.delete() || !file.exists()) {
                synchronized (DELETE_ON_EXIT_HOOK_CLASS) {
                    DELETE_ON_EXIT_HOOK_DOT_FILES.remove(path);
                }
            }
        }
    }

    @SuppressWarnings("ConstantConditions")
    private static void cleanupSwingDataStructures() throws Exception {
        Object manager = ReflectionUtil.getDeclaredMethod(Class.forName("javax.swing.KeyboardManager"), "getCurrentManager").invoke(null);
        Map componentKeyStrokeMap = ReflectionUtil.getField(manager.getClass(), manager, Hashtable.class, "componentKeyStrokeMap");
        componentKeyStrokeMap.clear();
        Map containerMap = ReflectionUtil.getField(manager.getClass(), manager, Hashtable.class, "containerMap");
        containerMap.clear();
    }

    @NotNull
    public Disposable getTestRootDisposable() {
        return myTestRootDisposable;
    }

    @Override
    protected void runTest() throws Throwable {
        final Throwable[] throwables = new Throwable[1];

        AtomicBoolean completed = new AtomicBoolean(false);
        Runnable runnable = () -> {
            try {
                //TestLoggerFactory.onTestStarted();
                super.runTest();
                TestLoggerFactory.onTestFinished(true);
                completed.set(true);
            }
            catch (InvocationTargetException e) {
                TestLoggerFactory.onTestFinished(false);
                e.fillInStackTrace();
                throwables[0] = e.getTargetException();
            }
            catch (IllegalAccessException e) {
                TestLoggerFactory.onTestFinished(false);
                e.fillInStackTrace();
                throwables[0] = e;
            }
            catch (Throwable e) {
                TestLoggerFactory.onTestFinished(false);
                throwables[0] = e;
            }
        };

        invokeTestRunnable(runnable);

        if (throwables[0] != null) {
            throw throwables[0];
        }
        if (!completed.get()) {
            throw new IllegalStateException("test didn't start");
        }
    }

    protected boolean shouldRunTest() {
        return TestFrameworkUtil.canRunTest(getClass());
    }

    protected void invokeTestRunnable(@NotNull Runnable runnable) throws Exception {
        //IdeaTestExecutionPolicy policy = IdeaTestExecutionPolicy.current();
        //if (policy != null && !policy.runInDispatchThread()) {
        //    runnable.run();
        //}
        //else {
            EdtTestUtilKt.runInEdtAndWait(() -> {
                runnable.run();
                return null;
            });
        //}
    }

    private void defaultRunBare() throws Throwable {
        Throwable exception = null;
        try {
            long setupStart = System.nanoTime();
            setUp();
            long setupCost = (System.nanoTime() - setupStart) / 1000000;
            logPerClassCost(setupCost, TOTAL_SETUP_COST_MILLIS);

            runTest();
        }
        catch (Throwable running) {
            exception = running;
        }
        finally {
            try {
                long teardownStart = System.nanoTime();
                tearDown();
                long teardownCost = (System.nanoTime() - teardownStart) / 1000000;
                logPerClassCost(teardownCost, TOTAL_TEARDOWN_COST_MILLIS);
            }
            catch (Throwable tearingDown) {
                if (exception == null) exception = tearingDown;
                else exception = new CompoundRuntimeException(Arrays.asList(exception, tearingDown));
            }
        }
        if (exception != null) throw exception;
    }

    /**
     * Logs the setup cost grouped by test fixture class (superclass of the current test class).
     *
     * @param cost setup cost in milliseconds
     */
    private void logPerClassCost(long cost, @NotNull Map<String, Long> costMap) {
        Class<?> superclass = getClass().getSuperclass();
        Long oldCost = costMap.get(superclass.getName());
        long newCost = oldCost == null ? cost : oldCost + cost;
        costMap.put(superclass.getName(), newCost);
    }

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    static void logSetupTeardownCosts() {
        System.out.println("Setup costs");
        long totalSetup = 0;
        for (Map.Entry<String, Long> entry : TOTAL_SETUP_COST_MILLIS.entrySet()) {
            System.out.println(String.format("  %s: %d ms", entry.getKey(), entry.getValue()));
            totalSetup += entry.getValue();
        }
        System.out.println("Teardown costs");
        long totalTeardown = 0;
        for (Map.Entry<String, Long> entry : TOTAL_TEARDOWN_COST_MILLIS.entrySet()) {
            System.out.println(String.format("  %s: %d ms", entry.getKey(), entry.getValue()));
            totalTeardown += entry.getValue();
        }
        System.out.println(String.format("Total overhead: setup %d ms, teardown %d ms", totalSetup, totalTeardown));
        System.out.println(String.format("##teamcity[buildStatisticValue key='ideaTests.totalSetupMs' value='%d']", totalSetup));
        System.out.println(String.format("##teamcity[buildStatisticValue key='ideaTests.totalTeardownMs' value='%d']", totalTeardown));
    }

    @Override
    public void runBare() throws Throwable {
        if (!shouldRunTest()) return;

        if (runInDispatchThread()) {
            TestRunnerUtil.replaceIdeEventQueueSafely();
            com.intellij.testFramework.EdtTestUtil.runInEdtAndWait(this::defaultRunBare);
        }
        else {
            defaultRunBare();
        }
    }

    protected boolean runInDispatchThread() {
        //IdeaTestExecutionPolicy policy = IdeaTestExecutionPolicy.current();
        //if (policy != null) {
        //    return policy.runInDispatchThread();
        //}
        return true;
    }

    @NotNull
    public static String toString(@NotNull Iterable<?> collection) {
        if (!collection.iterator().hasNext()) {
            return "<empty>";
        }

        final StringBuilder builder = new StringBuilder();
        for (final Object o : collection) {
            if (o instanceof THashSet) {
                builder.append(new TreeSet<>((THashSet<?>)o));
            }
            else {
                builder.append(o);
            }
            builder.append('\n');
        }
        return builder.toString();
    }

    @SafeVarargs
    public static <T> void assertOrderedEquals(@NotNull T[] actual, @NotNull T... expected) {
        assertOrderedEquals(Arrays.asList(actual), expected);
    }

    @SafeVarargs
    public static <T> void assertOrderedEquals(@NotNull Iterable<? extends T> actual, @NotNull T... expected) {
        assertOrderedEquals("", actual, expected);
    }

    public static void assertOrderedEquals(@NotNull byte[] actual, @NotNull byte[] expected) {
        assertEquals(expected.length, actual.length);
        for (int i = 0; i < actual.length; i++) {
            byte a = actual[i];
            byte e = expected[i];
            assertEquals("not equals at index: "+i, e, a);
        }
    }

    public static void assertOrderedEquals(@NotNull int[] actual, @NotNull int[] expected) {
        if (actual.length != expected.length) {
            fail("Expected size: "+expected.length+"; actual: "+actual.length+"\nexpected: "+Arrays.toString(expected)+"\nactual  : "+Arrays.toString(actual));
        }
        for (int i = 0; i < actual.length; i++) {
            int a = actual[i];
            int e = expected[i];
            assertEquals("not equals at index: "+i, e, a);
        }
    }

    @SafeVarargs
    public static <T> void assertOrderedEquals(@NotNull String errorMsg, @NotNull Iterable<? extends T> actual, @NotNull T... expected) {
        assertOrderedEquals(errorMsg, actual, Arrays.asList(expected));
    }

    public static <T> void assertOrderedEquals(@NotNull Iterable<? extends T> actual, @NotNull Iterable<? extends T> expected) {
        assertOrderedEquals("", actual, expected);
    }

    public static <T> void assertOrderedEquals(@NotNull String errorMsg,
            @NotNull Iterable<? extends T> actual,
            @NotNull Iterable<? extends T> expected) {
        //noinspection unchecked
        assertOrderedEquals(errorMsg, actual, expected, Equality.CANONICAL);
    }

    public static <T> void assertOrderedEquals(@NotNull String errorMsg,
            @NotNull Iterable<? extends T> actual,
            @NotNull Iterable<? extends T> expected,
            @NotNull Equality<? super T> comparator) {
        if (!equals(actual, expected, comparator)) {
            String expectedString = toString(expected);
            String actualString = toString(actual);
            Assert.assertEquals(errorMsg, expectedString, actualString);
            Assert.fail("Warning! 'toString' does not reflect the difference.\nExpected: " + expectedString + "\nActual: " + actualString);
        }
    }

    private static <T> boolean equals(@NotNull Iterable<? extends T> a1,
            @NotNull Iterable<? extends T> a2,
            @NotNull Equality<? super T> comparator) {
        Iterator<? extends T> it1 = a1.iterator();
        Iterator<? extends T> it2 = a2.iterator();
        while (it1.hasNext() || it2.hasNext()) {
            if (!it1.hasNext() || !it2.hasNext()) return false;
            if (!comparator.equals(it1.next(), it2.next())) return false;
        }
        return true;
    }

    @SafeVarargs
    public static <T> void assertOrderedCollection(@NotNull T[] collection, @NotNull Consumer<T>... checkers) {
        assertOrderedCollection(Arrays.asList(collection), checkers);
    }

    /**
     * Checks {@code actual} contains same elements (in {@link #equals(Object)} meaning) as {@code expected} irrespective of their order
     */
    @SafeVarargs
    public static <T> void assertSameElements(@NotNull T[] actual, @NotNull T... expected) {
        assertSameElements(Arrays.asList(actual), expected);
    }

    /**
     * Checks {@code actual} contains same elements (in {@link #equals(Object)} meaning) as {@code expected} irrespective of their order
     */
    @SafeVarargs
    public static <T> void assertSameElements(@NotNull Collection<? extends T> actual, @NotNull T... expected) {
        assertSameElements(actual, Arrays.asList(expected));
    }

    /**
     * Checks {@code actual} contains same elements (in {@link #equals(Object)} meaning) as {@code expected} irrespective of their order
     */
    public static <T> void assertSameElements(@NotNull Collection<? extends T> actual, @NotNull Collection<? extends T> expected) {
        assertSameElements("", actual, expected);
    }

    /**
     * Checks {@code actual} contains same elements (in {@link #equals(Object)} meaning) as {@code expected} irrespective of their order
     */
    public static <T> void assertSameElements(@NotNull String message, @NotNull Collection<? extends T> actual, @NotNull Collection<? extends T> expected) {
        if (actual.size() != expected.size() || !new HashSet<>(expected).equals(new HashSet<T>(actual))) {
            Assert.assertEquals(message, new HashSet<>(expected), new HashSet<T>(actual));
        }
    }

    @SafeVarargs
    public static <T> void assertContainsOrdered(@NotNull Collection<? extends T> collection, @NotNull T... expected) {
        assertContainsOrdered(collection, Arrays.asList(expected));
    }

    public static <T> void assertContainsOrdered(@NotNull Collection<? extends T> collection, @NotNull Collection<? extends T> expected) {
        ArrayList<T> copy = new ArrayList<>(collection);
        copy.retainAll(expected);
        assertOrderedEquals(toString(collection), copy, expected);
    }

    @SafeVarargs
    public static <T> void assertContainsElements(@NotNull Collection<? extends T> collection, @NotNull T... expected) {
        assertContainsElements(collection, Arrays.asList(expected));
    }

    public static <T> void assertContainsElements(@NotNull Collection<? extends T> collection, @NotNull Collection<? extends T> expected) {
        ArrayList<T> copy = new ArrayList<>(collection);
        copy.retainAll(expected);
        assertSameElements(toString(collection), copy, expected);
    }

    @NotNull
    public static String toString(@NotNull Object[] collection, @NotNull String separator) {
        return toString(Arrays.asList(collection), separator);
    }

    @SafeVarargs
    public static <T> void assertDoesntContain(@NotNull Collection<? extends T> collection, @NotNull T... notExpected) {
        assertDoesntContain(collection, Arrays.asList(notExpected));
    }

    public static <T> void assertDoesntContain(@NotNull Collection<? extends T> collection, @NotNull Collection<? extends T> notExpected) {
        ArrayList<T> expected = new ArrayList<>(collection);
        expected.removeAll(notExpected);
        assertSameElements(collection, expected);
    }

    @NotNull
    public static String toString(@NotNull Collection<?> collection, @NotNull String separator) {
        List<String> list = ContainerUtil.map2List(collection, String::valueOf);
        Collections.sort(list);
        StringBuilder builder = new StringBuilder();
        boolean flag = false;
        for (final String o : list) {
            if (flag) {
                builder.append(separator);
            }
            builder.append(o);
            flag = true;
        }
        return builder.toString();
    }

    @SafeVarargs
    public static <T> void assertOrderedCollection(@NotNull Collection<? extends T> collection, @NotNull Consumer<T>... checkers) {
        if (collection.size() != checkers.length) {
            Assert.fail(toString(collection));
        }
        int i = 0;
        for (final T actual : collection) {
            try {
                checkers[i].consume(actual);
            }
            catch (AssertionFailedError e) {
                //noinspection UseOfSystemOutOrSystemErr
                System.out.println(i + ": " + actual);
                throw e;
            }
            i++;
        }
    }

    @SafeVarargs
    public static <T> void assertUnorderedCollection(@NotNull T[] collection, @NotNull Consumer<T>... checkers) {
        assertUnorderedCollection(Arrays.asList(collection), checkers);
    }

    @SafeVarargs
    public static <T> void assertUnorderedCollection(@NotNull Collection<? extends T> collection, @NotNull Consumer<T>... checkers) {
        if (collection.size() != checkers.length) {
            Assert.fail(toString(collection));
        }
        Set<Consumer<T>> checkerSet = new HashSet<>(Arrays.asList(checkers));
        int i = 0;
        Throwable lastError = null;
        for (final T actual : collection) {
            boolean flag = true;
            for (final Consumer<T> condition : checkerSet) {
                Throwable error = accepts(condition, actual);
                if (error == null) {
                    checkerSet.remove(condition);
                    flag = false;
                    break;
                }
                else {
                    lastError = error;
                }
            }
            if (flag) {
                //noinspection ConstantConditions,CallToPrintStackTrace
                lastError.printStackTrace();
                Assert.fail("Incorrect element(" + i + "): " + actual);
            }
            i++;
        }
    }

    private static <T> Throwable accepts(@NotNull Consumer<? super T> condition, final T actual) {
        try {
            condition.consume(actual);
            return null;
        }
        catch (Throwable e) {
            return e;
        }
    }

    @Contract("null, _ -> fail")
    @NotNull
    public static <T> T assertInstanceOf(Object o, @NotNull Class<T> aClass) {
        Assert.assertNotNull("Expected instance of: " + aClass.getName() + " actual: " + null, o);
        Assert.assertTrue("Expected instance of: " + aClass.getName() + " actual: " + o.getClass().getName(), aClass.isInstance(o));
        @SuppressWarnings("unchecked") T t = (T)o;
        return t;
    }

    public static <T> T assertOneElement(@NotNull Collection<? extends T> collection) {
        Iterator<? extends T> iterator = collection.iterator();
        String toString = toString(collection);
        Assert.assertTrue(toString, iterator.hasNext());
        T t = iterator.next();
        Assert.assertFalse(toString, iterator.hasNext());
        return t;
    }

    public static <T> T assertOneElement(@NotNull T[] ts) {
        Assert.assertEquals(Arrays.asList(ts).toString(), 1, ts.length);
        return ts[0];
    }

    @SafeVarargs
    public static <T> void assertOneOf(T value, @NotNull T... values) {
        for (T v : values) {
            if (Objects.equals(value, v)) {
                return;
            }
        }
        Assert.fail(value + " should be equal to one of " + Arrays.toString(values));
    }

    public static void printThreadDump() {
        PerformanceWatcher.dumpThreadsToConsole("Thread dump:");
    }

    public static void assertEmpty(@NotNull Object[] array) {
        assertOrderedEquals(array);
    }

    public static void assertNotEmpty(final Collection<?> collection) {
        assertNotNull(collection);
        assertFalse(collection.isEmpty());
    }

    public static void assertEmpty(@NotNull Collection<?> collection) {
        assertEmpty(collection.toString(), collection);
    }

    public static void assertNullOrEmpty(@Nullable Collection<?> collection) {
        if (collection == null) return;
        assertEmpty("", collection);
    }

    public static void assertEmpty(final String s) {
        assertTrue(s, StringUtil.isEmpty(s));
    }

    public static <T> void assertEmpty(@NotNull String errorMsg, @NotNull Collection<? extends T> collection) {
        assertOrderedEquals(errorMsg, collection, Collections.emptyList());
    }

    public static void assertSize(int expectedSize, @NotNull Object[] array) {
        if (array.length != expectedSize) {
            assertEquals(toString(Arrays.asList(array)), expectedSize, array.length);
        }
    }

    public static void assertSize(int expectedSize, @NotNull Collection<?> c) {
        if (c.size() != expectedSize) {
            assertEquals(toString(c), expectedSize, c.size());
        }
    }

    @NotNull
    protected <T extends Disposable> T disposeOnTearDown(@NotNull T disposable) {
        Disposer.register(getTestRootDisposable(), disposable);
        return disposable;
    }

    public static void assertSameLines(@NotNull String expected, @NotNull String actual) {
        assertSameLines(null, expected, actual);
    }

    public static void assertSameLines(@Nullable String message, @NotNull String expected, @NotNull String actual) {
        String expectedText = StringUtil.convertLineSeparators(expected.trim());
        String actualText = StringUtil.convertLineSeparators(actual.trim());
        Assert.assertEquals(message, expectedText, actualText);
    }

    public static void assertExists(@NotNull File file){
        assertTrue("File should exist " + file, file.exists());
    }

    public static void assertDoesntExist(@NotNull File file){
        assertFalse("File should not exist " + file, file.exists());
    }

    @NotNull
    protected String getTestName(boolean lowercaseFirstLetter) {
        return getTestName(getName(), lowercaseFirstLetter);
    }

    @NotNull
    public static String getTestName(@Nullable String name, boolean lowercaseFirstLetter) {
        return name == null ? "" : PlatformTestUtil.getTestName(name, lowercaseFirstLetter);
    }

    @NotNull
    protected String getTestDirectoryName() {
        final String testName = getTestName(true);
        return testName.replaceAll("_.*", "");
    }

    public static void assertSameLinesWithFile(@NotNull String filePath, @NotNull String actualText) {
        assertSameLinesWithFile(filePath, actualText, true);
    }

    public static void assertSameLinesWithFile(@NotNull String filePath,
            @NotNull String actualText,
            @NotNull Supplier<String> messageProducer) {
        assertSameLinesWithFile(filePath, actualText, true, messageProducer);
    }

    public static void assertSameLinesWithFile(@NotNull String filePath, @NotNull String actualText, boolean trimBeforeComparing) {
        assertSameLinesWithFile(filePath, actualText, trimBeforeComparing, null);
    }

    public static void assertSameLinesWithFile(@NotNull String filePath,
            @NotNull String actualText,
            boolean trimBeforeComparing,
            @Nullable Supplier<String> messageProducer) {
        String fileText;
        try {
            if (OVERWRITE_TESTDATA) {
                VfsTestUtil.overwriteTestData(filePath, actualText);
                //noinspection UseOfSystemOutOrSystemErr
                System.out.println("File " + filePath + " created.");
            }
            fileText = FileUtil.loadFile(new File(filePath), CharsetToolkit.UTF8_CHARSET);
        }
        catch (FileNotFoundException e) {
            VfsTestUtil.overwriteTestData(filePath, actualText);
            throw new AssertionFailedError("No output text found. File " + filePath + " created.");
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        String expected = StringUtil.convertLineSeparators(trimBeforeComparing ? fileText.trim() : fileText);
        String actual = StringUtil.convertLineSeparators(trimBeforeComparing ? actualText.trim() : actualText);
        if (!Comparing.equal(expected, actual)) {
            throw new FileComparisonFailure(messageProducer == null ? null : messageProducer.get(), expected, actual, filePath);
        }
    }

    protected static void clearFields(@NotNull Object test) throws IllegalAccessException {
        Class aClass = test.getClass();
        while (aClass != null) {
            clearDeclaredFields(test, aClass);
            aClass = aClass.getSuperclass();
        }
    }

    public static void clearDeclaredFields(@NotNull Object test, @NotNull Class aClass) throws IllegalAccessException {
        for (final Field field : aClass.getDeclaredFields()) {
            final String name = field.getDeclaringClass().getName();
            if (!name.startsWith("junit.framework.") && !name.startsWith("com.intellij.testFramework.")) {
                final int modifiers = field.getModifiers();
                if ((modifiers & Modifier.FINAL) == 0 && (modifiers & Modifier.STATIC) == 0 && !field.getType().isPrimitive()) {
                    field.setAccessible(true);
                    field.set(test, null);
                }
            }
        }
    }

    private static void checkCodeStyleSettingsEqual(@NotNull CodeStyleSettings expected, @NotNull CodeStyleSettings settings) {
        if (!expected.equals(settings)) {
            Element oldS = new Element("temp");
            expected.writeExternal(oldS);
            Element newS = new Element("temp");
            settings.writeExternal(newS);

            String newString = JDOMUtil.writeElement(newS);
            String oldString = JDOMUtil.writeElement(oldS);
            Assert.assertEquals("Code style settings damaged", oldString, newString);
        }
    }

    public boolean isPerformanceTest() {
        String testName = getName();
        String className = getClass().getSimpleName();
        return TestFrameworkUtil.isPerformanceTest(testName, className);
    }

    /**
     * @return true for a test which performs A LOT of computations.
     * Such test should typically avoid performing expensive checks, e.g. data structure consistency complex validations.
     * If you want your test to be treated as "Stress", please mention one of these words in its name: "Stress", "Slow".
     * For example: {@code public void testStressPSIFromDifferentThreads()}
     */
    public boolean isStressTest() {
        return isStressTest(getName(), getClass().getName());
    }

    private static boolean isStressTest(String testName, String className) {
        return TestFrameworkUtil.isPerformanceTest(testName, className) ||
               containsStressWords(testName) ||
               containsStressWords(className);
    }

    private static boolean containsStressWords(@Nullable String name) {
        return name != null && (name.contains("Stress") || name.contains("Slow"));
    }


    /**
     * Checks that code block throw corresponding exception with expected error msg.
     * If expected error message is null it will not be checked.
     *
     * @param exceptionCase    Block annotated with some exception type
     * @param expectedErrorMsg expected error message
     */
    protected void assertException(@NotNull AbstractExceptionCase exceptionCase, @Nullable String expectedErrorMsg) {
        //noinspection unchecked
        assertExceptionOccurred(true, exceptionCase, expectedErrorMsg);
    }

    /**
     * Checks that code block doesn't throw corresponding exception.
     *
     * @param exceptionCase Block annotated with some exception type
     */
    protected <T extends Throwable> void assertNoException(@NotNull AbstractExceptionCase<T> exceptionCase) throws T {
        assertExceptionOccurred(false, exceptionCase, null);
    }

    protected void assertNoThrowable(@NotNull Runnable closure) {
        String throwableName = null;
        try {
            closure.run();
        }
        catch (Throwable thr) {
            throwableName = thr.getClass().getName();
        }
        assertNull(throwableName);
    }

    private static <T extends Throwable> void assertExceptionOccurred(boolean shouldOccur,
            @NotNull AbstractExceptionCase<T> exceptionCase,
            String expectedErrorMsg) throws T {
        boolean wasThrown = false;
        try {
            exceptionCase.tryClosure();
        }
        catch (Throwable e) {
            if (shouldOccur) {
                wasThrown = true;
                final String errorMessage = exceptionCase.getAssertionErrorMessage();
                assertEquals(errorMessage, exceptionCase.getExpectedExceptionClass(), e.getClass());
                if (expectedErrorMsg != null) {
                    assertEquals("Compare error messages", expectedErrorMsg, e.getMessage());
                }
            }
            else if (exceptionCase.getExpectedExceptionClass().equals(e.getClass())) {
                wasThrown = true;

                //noinspection UseOfSystemOutOrSystemErr
                System.out.println();
                //noinspection UseOfSystemOutOrSystemErr
                e.printStackTrace(System.out);

                fail("Exception isn't expected here. Exception message: " + e.getMessage());
            }
            else {
                throw e;
            }
        }
        finally {
            if (shouldOccur && !wasThrown) {
                fail(exceptionCase.getAssertionErrorMessage());
            }
        }
    }

    protected boolean annotatedWith(@NotNull Class<? extends Annotation> annotationClass) {
        Class<?> aClass = getClass();
        String methodName = "test" + getTestName(false);
        boolean methodChecked = false;
        while (aClass != null && aClass != Object.class) {
            if (aClass.getAnnotation(annotationClass) != null) return true;
            if (!methodChecked) {
                Method method = ReflectionUtil.getDeclaredMethod(aClass, methodName);
                if (method != null) {
                    if (method.getAnnotation(annotationClass) != null) return true;
                    methodChecked = true;
                }
            }
            aClass = aClass.getSuperclass();
        }
        return false;
    }

    @NotNull
    protected String getHomePath() {
        return PathManager.getHomePath().replace(File.separatorChar, '/');
    }

    public static void refreshRecursively(@NotNull VirtualFile file) {
        VfsUtilCore.visitChildrenRecursively(file, new VirtualFileVisitor() {
            @Override
            public boolean visitFile(@NotNull VirtualFile file) {
                file.getChildren();
                return true;
            }
        });
        file.refresh(false, true);
    }

    @Nullable
    public static VirtualFile refreshAndFindFile(@NotNull final File file) {
        return UIUtil.invokeAndWaitIfNeeded(() -> LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file));
    }

    protected class TestDisposable implements Disposable {
        private volatile boolean myDisposed;

        public TestDisposable() {
        }

        @Override
        public void dispose() {
            myDisposed = true;
        }

        public boolean isDisposed() {
            return myDisposed;
        }

        @Override
        public String toString() {
            String testName = getTestName(false);
            return KtUsefulTestCase.this.getClass() + (StringUtil.isEmpty(testName) ? "" : ".test" + testName);
        }
    };
}
