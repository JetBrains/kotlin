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

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.impl.ApplicationInfoImpl;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.io.FileSystemUtil;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.rt.execution.junit.FileComparisonFailure;
import com.intellij.util.ReflectionUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.hash.HashMap;
import com.intellij.util.ui.UIUtil;
import gnu.trove.THashSet;
import junit.framework.AssertionFailedError;
import junit.framework.TestCase;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.types.FlexibleTypeImpl;
import org.jetbrains.kotlin.utils.ExceptionUtilsKt;
import org.junit.Assert;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.*;

@SuppressWarnings("UseOfSystemOutOrSystemErr")
public abstract class KtUsefulTestCase extends TestCase {
    private static final String TEMP_DIR_MARKER = "unitTest_";

    private static final String ORIGINAL_TEMP_DIR = FileUtil.getTempDirectory();

    private static final Map<String, Long> TOTAL_SETUP_COST_MILLIS = new HashMap<>();
    private static final Map<String, Long> TOTAL_TEARDOWN_COST_MILLIS = new HashMap<>();

    private Application application;

    @NotNull
    protected final Disposable myTestRootDisposable = new TestDisposable();

    private static final String ourPathToKeep = null;
    private final List<String> myPathsToKeep = new ArrayList<>();

    private String myTempDir;

    static {
        // Radar #5755208: Command line Java applications need a way to launch without a Dock icon.
        System.setProperty("apple.awt.UIElement", "true");

        FlexibleTypeImpl.RUN_SLOW_ASSERTIONS = true;
    }

    private boolean oldDisposerDebug;

    @Override
    protected void setUp() throws Exception {
        application = ApplicationManager.getApplication();

        super.setUp();

        String testName =  FileUtil.sanitizeFileName(getTestName(true));
        if (StringUtil.isEmptyOrSpaces(testName)) testName = "";
        testName = new File(testName).getName(); // in case the test name contains file separators
        myTempDir = new File(ORIGINAL_TEMP_DIR, TEMP_DIR_MARKER + testName).getPath();
        FileUtil.resetCanonicalTempPathCache(myTempDir);
        boolean isStressTest = isStressTest();
        ApplicationInfoImpl.setInStressTest(isStressTest);
        // turn off Disposer debugging for performance tests
        oldDisposerDebug = Disposer.setDebugMode(Disposer.isDebugMode() && !isStressTest);
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            Disposer.dispose(myTestRootDisposable);
            cleanupSwingDataStructures();
            cleanupDeleteOnExitHookList();
        }
        finally {
            Disposer.setDebugMode(oldDisposerDebug);
            FileUtil.resetCanonicalTempPathCache(ORIGINAL_TEMP_DIR);
            if (hasTmpFilesToKeep()) {
                File[] files = new File(myTempDir).listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (!shouldKeepTmpFile(file)) {
                            FileUtil.delete(file);
                        }
                    }
                }
            }
            else {
                FileUtil.delete(new File(myTempDir));
            }
        }

        UIUtil.removeLeakingAppleListeners();
        super.tearDown();

        resetApplicationToNull(application);

        application = null;
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

    private boolean hasTmpFilesToKeep() {
        return !myPathsToKeep.isEmpty();
    }

    private boolean shouldKeepTmpFile(File file) {
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
        Set<String> files = ReflectionUtil.getStaticFieldValue(aClass, Set.class, "files");
        DELETE_ON_EXIT_HOOK_CLASS = aClass;
        DELETE_ON_EXIT_HOOK_DOT_FILES = files;
    }

    private static void cleanupDeleteOnExitHookList() throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        // try to reduce file set retained by java.io.DeleteOnExitHook
        List<String> list;
        synchronized (DELETE_ON_EXIT_HOOK_CLASS) {
            if (DELETE_ON_EXIT_HOOK_DOT_FILES.isEmpty()) return;
            list = new ArrayList<>(DELETE_ON_EXIT_HOOK_DOT_FILES);
        }
        for (int i = list.size() - 1; i >= 0; i--) {
            String path = list.get(i);
            if (FileSystemUtil.getAttributes(path) == null || new File(path).delete()) {
                synchronized (DELETE_ON_EXIT_HOOK_CLASS) {
                    DELETE_ON_EXIT_HOOK_DOT_FILES.remove(path);
                }
            }
        }
    }

    private static void cleanupSwingDataStructures() throws Exception {
        Object manager = ReflectionUtil.getDeclaredMethod(Class.forName("javax.swing.KeyboardManager"), "getCurrentManager").invoke(null);
        Map componentKeyStrokeMap = ReflectionUtil.getField(manager.getClass(), manager, Hashtable.class, "componentKeyStrokeMap");
        componentKeyStrokeMap.clear();
        Map containerMap = ReflectionUtil.getField(manager.getClass(), manager, Hashtable.class, "containerMap");
        containerMap.clear();
    }

    @NotNull
    public final Disposable getTestRootDisposable() {
        return myTestRootDisposable;
    }

    @Override
    protected void runTest() throws Throwable {
        Throwable[] throwables = new Throwable[1];

        Runnable runnable = () -> {
            try {
                super.runTest();
            }
            catch (InvocationTargetException e) {
                e.fillInStackTrace();
                throwables[0] = e.getTargetException();
            }
            catch (IllegalAccessException e) {
                e.fillInStackTrace();
                throwables[0] = e;
            }
            catch (Throwable e) {
                throwables[0] = e;
            }
        };

        invokeTestRunnable(runnable);

        if (throwables[0] != null) {
            throw throwables[0];
        }
    }

    private static void invokeTestRunnable(@NotNull Runnable runnable) throws Exception {
        EdtTestUtil.runInEdtAndWait(runnable);
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
            }
        }
        if (exception != null) throw exception;
    }

    /**
     * Logs the setup cost grouped by test fixture class (superclass of the current test class).
     *
     * @param cost setup cost in milliseconds
     */
    private void logPerClassCost(long cost, Map<String, Long> costMap) {
        Class<?> superclass = getClass().getSuperclass();
        Long oldCost = costMap.get(superclass.getName());
        long newCost = oldCost == null ? cost : oldCost + cost;
        costMap.put(superclass.getName(), newCost);
    }

    @Override
    public void runBare() throws Throwable {
        this.defaultRunBare();
    }

    @NonNls
    public static String toString(Iterable<?> collection) {
        if (!collection.iterator().hasNext()) {
            return "<empty>";
        }

        StringBuilder builder = new StringBuilder();
        for (Object o : collection) {
            if (o instanceof THashSet) {
                builder.append(new TreeSet<Object>((THashSet)o));
            }
            else {
                builder.append(o);
            }
            builder.append("\n");
        }
        return builder.toString();
    }

    private static <T> void assertOrderedEquals(String errorMsg, @NotNull Iterable<T> actual, @NotNull T... expected) {
        Assert.assertNotNull(actual);
        Assert.assertNotNull(expected);
        assertOrderedEquals(errorMsg, actual, Arrays.asList(expected));
    }

    public static <T> void assertOrderedEquals(
            String erroMsg,
            Iterable<? extends T> actual,
            Collection<? extends T> expected) {
        ArrayList<T> list = new ArrayList<>();
        for (T t : actual) {
            list.add(t);
        }
        if (!list.equals(new ArrayList<T>(expected))) {
            String expectedString = toString(expected);
            String actualString = toString(actual);
            Assert.assertEquals(erroMsg, expectedString, actualString);
            Assert.fail("Warning! 'toString' does not reflect the difference.\nExpected: " + expectedString + "\nActual: " + actualString);
        }
    }

    public static <T> void assertSameElements(T[] collection, T... expected) {
        assertSameElements(Arrays.asList(collection), expected);
    }

    public static <T> void assertSameElements(Collection<? extends T> collection, T... expected) {
        assertSameElements(collection, Arrays.asList(expected));
    }

    public static <T> void assertSameElements(Collection<? extends T> collection, Collection<T> expected) {
        assertSameElements(null, collection, expected);
    }

    public static <T> void assertSameElements(String message, Collection<? extends T> collection, Collection<T> expected) {
        assertNotNull(collection);
        assertNotNull(expected);
        if (collection.size() != expected.size() || !new HashSet<>(expected).equals(new HashSet<T>(collection))) {
            Assert.assertEquals(message, toString(expected, "\n"), toString(collection, "\n"));
            Assert.assertEquals(message, new HashSet<>(expected), new HashSet<T>(collection));
        }
    }

    public static String toString(Object[] collection, String separator) {
        return toString(Arrays.asList(collection), separator);
    }

    public static String toString(Collection<?> collection, String separator) {
        List<String> list = ContainerUtil.map2List(collection, String::valueOf);
        Collections.sort(list);
        StringBuilder builder = new StringBuilder();
        boolean flag = false;
        for (String o : list) {
            if (flag) {
                builder.append(separator);
            }
            builder.append(o);
            flag = true;
        }
        return builder.toString();
    }

    @Contract("null, _ -> fail")
    public static <T> T assertInstanceOf(Object o, Class<T> aClass) {
        Assert.assertNotNull("Expected instance of: " + aClass.getName() + " actual: " + null, o);
        Assert.assertTrue("Expected instance of: " + aClass.getName() + " actual: " + o.getClass().getName(), aClass.isInstance(o));
        @SuppressWarnings("unchecked") T t = (T)o;
        return t;
    }

    protected static <T> void assertEmpty(String errorMsg, Collection<T> collection) {
        //noinspection unchecked
        assertOrderedEquals(errorMsg, collection);
    }

    protected static void assertSize(int expectedSize, Object[] array) {
        assertEquals(toString(Arrays.asList(array)), expectedSize, array.length);
    }

    public static void assertSameLines(String expected, String actual) {
        String expectedText = StringUtil.convertLineSeparators(expected.trim());
        String actualText = StringUtil.convertLineSeparators(actual.trim());
        Assert.assertEquals(expectedText, actualText);
    }

    protected String getTestName(boolean lowercaseFirstLetter) {
        return getTestName(getName(), lowercaseFirstLetter);
    }

    public static String getTestName(String name, boolean lowercaseFirstLetter) {
        return name == null ? "" : KtPlatformTestUtil.getTestName(name, lowercaseFirstLetter);
    }

    /** @deprecated use {@link KtPlatformTestUtil#lowercaseFirstLetter(String, boolean)} (to be removed in IDEA 17) */
    @SuppressWarnings("unused")
    public static String lowercaseFirstLetter(String name, boolean lowercaseFirstLetter) {
        return KtPlatformTestUtil.lowercaseFirstLetter(name, lowercaseFirstLetter);
    }

    /** @deprecated use {@link KtPlatformTestUtil#isAllUppercaseName(String)} (to be removed in IDEA 17) */
    @SuppressWarnings("unused")
    public static boolean isAllUppercaseName(String name) {
        return KtPlatformTestUtil.isAllUppercaseName(name);
    }

    public static void assertSameLinesWithFile(String filePath, String actualText) {
        assertSameLinesWithFile(filePath, actualText, true);
    }

    public static void assertSameLinesWithFile(String filePath, String actualText, boolean trimBeforeComparing) {
        String fileText;
        try {
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
            throw new FileComparisonFailure(null, expected, actual, filePath);
        }
    }

    public static void clearFields(Object test) throws IllegalAccessException {
        Class aClass = test.getClass();
        while (aClass != null) {
            clearDeclaredFields(test, aClass);
            aClass = aClass.getSuperclass();
        }
    }

    private static void clearDeclaredFields(Object test, Class aClass) throws IllegalAccessException {
        if (aClass == null) return;
        for (Field field : aClass.getDeclaredFields()) {
            @NonNls String name = field.getDeclaringClass().getName();
            if (!name.startsWith("junit.framework.") && !name.startsWith("com.intellij.testFramework.")) {
                int modifiers = field.getModifiers();
                if ((modifiers & Modifier.FINAL) == 0 && (modifiers & Modifier.STATIC) == 0 && !field.getType().isPrimitive()) {
                    field.setAccessible(true);
                    field.set(test, null);
                }
            }
        }
    }

    private static boolean isPerformanceTest(@Nullable String testName, @Nullable String className) {
        return testName != null && testName.contains("Performance") ||
               className != null && className.contains("Performance");
    }

    /**
     * @return true for a test which performs A LOT of computations.
     * Such test should typically avoid performing expensive checks, e.g. data structure consistency complex validations.
     * If you want your test to be treated as "Stress", please mention one of these words in its name: "Stress", "Slow".
     * For example: {@code public void testStressPSIFromDifferentThreads()}
     */

    private boolean isStressTest() {
        return isStressTest(getName(), getClass().getName());
    }

    private static boolean isStressTest(String testName, String className) {
        return isPerformanceTest(testName, className) ||
               containsStressWords(testName) ||
               containsStressWords(className);
    }

    private static boolean containsStressWords(@Nullable String name) {
        return name != null && (name.contains("Stress") || name.contains("Slow"));
    }


    public class TestDisposable implements Disposable {
        @Override
        public void dispose() {
        }

        @Override
        public String toString() {
            String testName = getTestName(false);
            return KtUsefulTestCase.this.getClass() + (StringUtil.isEmpty(testName) ? "" : ".test" + testName);
        }
    };
}