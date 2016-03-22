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
import com.intellij.mock.MockApplication;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.application.impl.ApplicationInfoImpl;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.util.*;
import com.intellij.openapi.util.io.FileSystemUtil;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.*;
import com.intellij.psi.codeStyle.CodeStyleSchemes;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.rt.execution.junit.FileComparisonFailure;
import com.intellij.util.Consumer;
import com.intellij.util.Function;
import com.intellij.util.ReflectionUtil;
import com.intellij.util.ThrowableRunnable;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.hash.HashMap;
import com.intellij.util.ui.UIUtil;
import gnu.trove.THashSet;
import junit.framework.AssertionFailedError;
import junit.framework.TestCase;
import org.jdom.Element;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.SecureRandom;
import java.util.*;
import java.util.List;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("ALL")
public abstract class KtUsefulTestCase extends TestCase {
    public static final boolean IS_UNDER_TEAMCITY = System.getenv("TEAMCITY_VERSION") != null;
    /** @deprecated */
    @Deprecated
    public static final String IDEA_MARKER_CLASS = "com.intellij.openapi.roots.IdeaModifiableModelsProvider";
    public static final String TEMP_DIR_MARKER = "unitTest_";
    protected static boolean OVERWRITE_TESTDATA = false;
    private static final String DEFAULT_SETTINGS_EXTERNALIZED;
    private static final Random RNG = new SecureRandom();
    private static final String ORIGINAL_TEMP_DIR = FileUtil.getTempDirectory();
    public static Map<String, Long> TOTAL_SETUP_COST_MILLIS = new HashMap();
    public static Map<String, Long> TOTAL_TEARDOWN_COST_MILLIS = new HashMap();
    protected final Disposable myTestRootDisposable = new Disposable() {
        public void dispose() {
        }

        public String toString() {
            String testName = KtUsefulTestCase.this.getTestName(false);
            return KtUsefulTestCase.this.getClass() + (StringUtil.isEmpty(testName) ? "" : ".test" + testName);
        }
    };
    protected static String ourPathToKeep = null;
    private List<String> myPathsToKeep = new ArrayList();
    private CodeStyleSettings myOldCodeStyleSettings;
    private String myTempDir;
    protected static final Key<String> CREATION_PLACE = Key.create("CREATION_PLACE");
    private static final Set<String> DELETE_ON_EXIT_HOOK_DOT_FILES;
    private static final Class DELETE_ON_EXIT_HOOK_CLASS;

    public KtUsefulTestCase() {
    }

    protected boolean shouldContainTempFiles() {
        return true;
    }

    protected void setUp() throws Exception {
        super.setUp();
        if(this.shouldContainTempFiles()) {
            String testName = FileUtil.sanitizeFileName(this.getTestName(true));
            if(StringUtil.isEmptyOrSpaces(testName)) {
                testName = "";
            }

            testName = (new File(testName)).getName();
            this.myTempDir = FileUtil.toSystemDependentName(ORIGINAL_TEMP_DIR + "/" + "unitTest_" + testName + "_" + RNG.nextInt(1000));
            FileUtil.resetCanonicalTempPathCache(this.myTempDir);
        }

        ApplicationInfoImpl.setInPerformanceTest(this.isPerformanceTest());
    }

    protected void tearDown() throws Exception {
        boolean var13 = false;

        try {
            var13 = true;
            Disposer.dispose(this.myTestRootDisposable);
            cleanupSwingDataStructures();
            cleanupDeleteOnExitHookList();
            var13 = false;
        } finally {
            if(var13) {
                if(this.shouldContainTempFiles()) {
                    FileUtil.resetCanonicalTempPathCache(ORIGINAL_TEMP_DIR);
                    if(this.hasTmpFilesToKeep()) {
                        File[] files1 = (new File(this.myTempDir)).listFiles();
                        if(files1 != null) {
                            File[] var8 = files1;
                            int var9 = files1.length;

                            for(int var10 = 0; var10 < var9; ++var10) {
                                File file1 = var8[var10];
                                if(!this.shouldKeepTmpFile(file1)) {
                                    FileUtil.delete(file1);
                                }
                            }
                        }
                    } else {
                        FileUtil.delete(new File(this.myTempDir));
                    }
                }

            }
        }

        if(this.shouldContainTempFiles()) {
            FileUtil.resetCanonicalTempPathCache(ORIGINAL_TEMP_DIR);
            if(this.hasTmpFilesToKeep()) {
                File[] files = (new File(this.myTempDir)).listFiles();
                if(files != null) {
                    File[] var2 = files;
                    int var3 = files.length;

                    for(int var4 = 0; var4 < var3; ++var4) {
                        File file = var2[var4];
                        if(!this.shouldKeepTmpFile(file)) {
                            FileUtil.delete(file);
                        }
                    }
                }
            } else {
                FileUtil.delete(new File(this.myTempDir));
            }
        }

        UIUtil.removeLeakingAppleListeners();
        super.tearDown();
    }

    protected void addTmpFileToKeep(File file) {
        this.myPathsToKeep.add(file.getPath());
    }

    private boolean hasTmpFilesToKeep() {
        return ourPathToKeep != null && FileUtil.isAncestor(this.myTempDir, ourPathToKeep, false) || !this.myPathsToKeep.isEmpty();
    }

    private boolean shouldKeepTmpFile(File file) {
        String path = file.getPath();
        if(FileUtil.pathsEqual(path, ourPathToKeep)) {
            return true;
        } else {
            Iterator var3 = this.myPathsToKeep.iterator();

            String pathToKeep;
            do {
                if(!var3.hasNext()) {
                    return false;
                }

                pathToKeep = (String)var3.next();
            } while(!FileUtil.pathsEqual(path, pathToKeep));

            return true;
        }
    }

    public static void cleanupDeleteOnExitHookList() throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        Class i = DELETE_ON_EXIT_HOOK_CLASS;
        ArrayList list;
        synchronized(DELETE_ON_EXIT_HOOK_CLASS) {
            if(DELETE_ON_EXIT_HOOK_DOT_FILES.isEmpty()) {
                return;
            }

            list = new ArrayList(DELETE_ON_EXIT_HOOK_DOT_FILES);
        }

        for(int var7 = list.size() - 1; var7 >= 0; --var7) {
            String path = (String)list.get(var7);
            if(FileSystemUtil.getAttributes(path) == null || (new File(path)).delete()) {
                Class var3 = DELETE_ON_EXIT_HOOK_CLASS;
                synchronized(DELETE_ON_EXIT_HOOK_CLASS) {
                    DELETE_ON_EXIT_HOOK_DOT_FILES.remove(path);
                }
            }
        }

    }

    private static void cleanupSwingDataStructures() throws Exception {
        Object manager = ReflectionUtil
                .getDeclaredMethod(Class.forName("javax.swing.KeyboardManager"), "getCurrentManager", new Class[0]).invoke((Object)null, new Object[0]);
        Map componentKeyStrokeMap = (Map)ReflectionUtil.getField(manager.getClass(), manager, Hashtable.class, "componentKeyStrokeMap");
        componentKeyStrokeMap.clear();
        Map containerMap = (Map)ReflectionUtil.getField(manager.getClass(), manager, Hashtable.class, "containerMap");
        containerMap.clear();
    }

    protected void checkForSettingsDamage(@NotNull List<Throwable> exceptions) {
        Application app = ApplicationManager.getApplication();
        if(!this.isPerformanceTest() && app != null && !(app instanceof MockApplication)) {
            CodeStyleSettings oldCodeStyleSettings = this.myOldCodeStyleSettings;
            if(oldCodeStyleSettings != null) {
                this.myOldCodeStyleSettings = null;
                doCheckForSettingsDamage(oldCodeStyleSettings, this.getCurrentCodeStyleSettings(), exceptions);
            }
        }
    }

    public static void doCheckForSettingsDamage(@NotNull CodeStyleSettings oldCodeStyleSettings, @NotNull CodeStyleSettings currentCodeStyleSettings, @NotNull List<Throwable> exceptions) {
        //CodeInsightSettings settings = CodeInsightSettings.getInstance();
        //
        //try {
        //    Element e = new Element("temp");
        //    settings.writeExternal(e);
        //    Assert.assertEquals("Code insight settings damaged", DEFAULT_SETTINGS_EXTERNALIZED, JDOMUtil.writeElement(e, "\n"));
        //} catch (AssertionError var23) {
        //    CodeInsightSettings clean = new CodeInsightSettings();
        //    Field[] var6 = clean.getClass().getFields();
        //    int var7 = var6.length;
        //
        //    for(int var8 = 0; var8 < var7; ++var8) {
        //        Field field = var6[var8];
        //
        //        try {
        //            ReflectionUtil.copyFieldValue(clean, settings, field);
        //        } catch (Exception var22) {
        //            ;
        //        }
        //    }
        //
        //    exceptions.add(var23);
        //}
        //
        //currentCodeStyleSettings.getIndentOptions(StdFileTypes.JAVA);
        //
        //try {
        //    checkSettingsEqual(oldCodeStyleSettings, currentCodeStyleSettings, "Code style settings damaged");
        //} catch (Throwable var20) {
        //    exceptions.add(var20);
        //} finally {
        //    currentCodeStyleSettings.clearCodeStyleSettings();
        //}
        //
        //try {
        //    InplaceRefactoring.checkCleared();
        //} catch (AssertionError var19) {
        //    exceptions.add(var19);
        //}
        //
        //try {
        //    StartMarkAction.checkCleared();
        //} catch (AssertionError var18) {
        //    exceptions.add(var18);
        //}

    }

    protected void storeSettings() {
        if(!this.isPerformanceTest() && ApplicationManager.getApplication() != null) {
            this.myOldCodeStyleSettings = this.getCurrentCodeStyleSettings().clone();
            this.myOldCodeStyleSettings.getIndentOptions(StdFileTypes.JAVA);
        }

    }

    protected CodeStyleSettings getCurrentCodeStyleSettings() {
        return CodeStyleSchemes.getInstance().getCurrentScheme() == null ? new CodeStyleSettings() : CodeStyleSettingsManager.getInstance().getCurrentSettings();
    }

    public Disposable getTestRootDisposable() {
        return this.myTestRootDisposable;
    }

    protected void runTest() throws Throwable {
        final Throwable[] throwables = new Throwable[1];
        Runnable runnable = new Runnable() {
            public void run() {
                try {
                    KtUsefulTestCase.super.runTest();
                } catch (InvocationTargetException var2) {
                    var2.fillInStackTrace();
                    throwables[0] = var2.getTargetException();
                } catch (IllegalAccessException var3) {
                    var3.fillInStackTrace();
                    throwables[0] = var3;
                } catch (Throwable var4) {
                    throwables[0] = var4;
                }

            }
        };
        this.invokeTestRunnable(runnable);
        if(throwables[0] != null) {
            throw throwables[0];
        }
    }

    protected boolean shouldRunTest() {
        return KtPlatformTestUtil.canRunTest(this.getClass());
    }

    public static void edt(@NotNull Runnable r) {
        EdtTestUtil.runInEdtAndWait(r);
    }

    protected void invokeTestRunnable(@NotNull Runnable runnable) throws Exception {
        EdtTestUtil.runInEdtAndWait(runnable);
    }

    protected void defaultRunBare() throws Throwable {
        Throwable exception = null;
        boolean var16 = false;

        label102: {
            long tearingDown;
            long teardownCost;
            label101: {
                try {
                    var16 = true;
                    tearingDown = System.nanoTime();
                    this.setUp();
                    teardownCost = (System.nanoTime() - tearingDown) / 1000000L;
                    this.logPerClassCost(teardownCost, TOTAL_SETUP_COST_MILLIS);
                    this.runTest();
                    var16 = false;
                    break label101;
                } catch (Throwable var20) {
                    exception = var20;
                    var16 = false;
                } finally {
                    if(var16) {
                        try {
                            long tearingDown1 = System.nanoTime();
                            this.tearDown();
                            long teardownCost1 = (System.nanoTime() - tearingDown1) / 1000000L;
                            this.logPerClassCost(teardownCost1, TOTAL_TEARDOWN_COST_MILLIS);
                        } catch (Throwable var17) {
                            if(exception == null) {
                                ;
                            }
                        }

                    }
                }

                try {
                    tearingDown = System.nanoTime();
                    this.tearDown();
                    teardownCost = (System.nanoTime() - tearingDown) / 1000000L;
                    this.logPerClassCost(teardownCost, TOTAL_TEARDOWN_COST_MILLIS);
                } catch (Throwable var18) {
                    if(exception == null) {
                        exception = var18;
                    }
                }
                break label102;
            }

            try {
                tearingDown = System.nanoTime();
                this.tearDown();
                teardownCost = (System.nanoTime() - tearingDown) / 1000000L;
                this.logPerClassCost(teardownCost, TOTAL_TEARDOWN_COST_MILLIS);
            } catch (Throwable var19) {
                if(exception == null) {
                    exception = var19;
                }
            }
        }

        if(exception != null) {
            throw exception;
        }
    }

    private void logPerClassCost(long cost, Map<String, Long> costMap) {
        Class superclass = this.getClass().getSuperclass();
        Long oldCost = (Long)costMap.get(superclass.getName());
        long newCost = oldCost == null?cost:oldCost.longValue() + cost;
        costMap.put(superclass.getName(), Long.valueOf(newCost));
    }

    public static void logSetupTeardownCosts() {
        long totalSetup = 0L;
        long totalTeardown = 0L;
        System.out.println("Setup costs");

        Iterator var4;
        Map.Entry entry;
        for(var4 = TOTAL_SETUP_COST_MILLIS.entrySet().iterator(); var4.hasNext(); totalSetup += ((Long)entry.getValue()).longValue()) {
            entry = (Map.Entry)var4.next();
            System.out.println(String.format("  %s: %d ms", new Object[]{entry.getKey(), entry.getValue()}));
        }

        System.out.println("Teardown costs");

        for(var4 = TOTAL_TEARDOWN_COST_MILLIS.entrySet().iterator(); var4.hasNext(); totalTeardown += ((Long)entry.getValue()).longValue()) {
            entry = (Map.Entry)var4.next();
            System.out.println(String.format("  %s: %d ms", new Object[]{entry.getKey(), entry.getValue()}));
        }

        System.out.println(String.format("Total overhead: setup %d ms, teardown %d ms", new Object[]{Long.valueOf(totalSetup), Long.valueOf(totalTeardown)}));
        System.out.println(String.format("##teamcity[buildStatisticValue key=\'ideaTests.totalSetupMs\' value=\'%d\']", new Object[]{Long.valueOf(totalSetup)}));
        System.out.println(String.format("##teamcity[buildStatisticValue key=\'ideaTests.totalTeardownMs\' value=\'%d\']", new Object[]{Long.valueOf(totalTeardown)}));
    }

    public void runBare() throws Throwable {
        if(this.shouldRunTest()) {
            if(this.runInDispatchThread()) {
                //TestRunnerUtil.replaceIdeEventQueueSafely();
                //EdtTestUtil.runInEdtAndWait(new ThrowableRunnable() {
                //    public void run() throws Throwable {
                        KtUsefulTestCase.this.defaultRunBare();
                //    }
                //});
            } else {
                this.defaultRunBare();
            }

        }
    }

    protected boolean runInDispatchThread() {
        return true;
    }

    @NonNls
    public static String toString(Iterable<?> collection) {
        if(!collection.iterator().hasNext()) {
            return "<empty>";
        } else {
            StringBuilder builder = new StringBuilder();

            for(Iterator var2 = collection.iterator(); var2.hasNext(); builder.append("\n")) {
                Object o = var2.next();
                if(o instanceof THashSet) {
                    builder.append(new TreeSet((THashSet)o));
                } else {
                    builder.append(o);
                }
            }

            return builder.toString();
        }
    }

    public static <T> void assertOrderedEquals(T[] actual, T... expected) {
        assertOrderedEquals((Iterable)Arrays.asList(actual), (Object[])expected);
    }

    public static <T> void assertOrderedEquals(Iterable<T> actual, T... expected) {
        assertOrderedEquals((String)null, actual, expected);
    }

    public static void assertOrderedEquals(@NotNull byte[] actual, @NotNull byte[] expected) {
        assertEquals(actual.length, expected.length);

        for(int i = 0; i < actual.length; ++i) {
            byte a = actual[i];
            byte e = expected[i];
            assertEquals("not equals at index: " + i, e, a);
        }

    }

    public static void assertOrderedEquals(@NotNull int[] actual, @NotNull int[] expected) {
        if(actual.length != expected.length) {
            fail("Expected size: " + expected.length + "; actual: " + actual.length + "\nexpected: " + Arrays.toString(expected) + "\nactual  : " + Arrays.toString(actual));
        }

        for(int i = 0; i < actual.length; ++i) {
            int a = actual[i];
            int e = expected[i];
            assertEquals("not equals at index: " + i, e, a);
        }

    }

    public static <T> void assertOrderedEquals(String errorMsg, @NotNull Iterable<T> actual, @NotNull T... expected) {
        Assert.assertNotNull(actual);
        Assert.assertNotNull(expected);
        assertOrderedEquals(errorMsg, actual, (Collection)Arrays.asList(expected));
    }

    public static <T> void assertOrderedEquals(Iterable<? extends T> actual, Collection<? extends T> expected) {
        assertOrderedEquals((String)null, actual, (Collection)expected);
    }

    public static <T> void assertOrderedEquals(String erroMsg, Iterable<? extends T> actual, Collection<? extends T> expected) {
        ArrayList list = new ArrayList();
        Iterator expectedString = actual.iterator();

        while(expectedString.hasNext()) {
            Object actualString = expectedString.next();
            list.add(actualString);
        }

        if(!list.equals(new ArrayList(expected))) {
            String expectedString1 = toString(expected);
            String actualString1 = toString(actual);
            Assert.assertEquals(erroMsg, expectedString1, actualString1);
            Assert.fail("Warning! \'toString\' does not reflect the difference.\nExpected: " + expectedString1 + "\nActual: " + actualString1);
        }

    }

    public static <T> void assertOrderedCollection(T[] collection, @NotNull Consumer... checkers) {
        Assert.assertNotNull(collection);
        assertOrderedCollection((Collection)Arrays.asList(collection), checkers);
    }

    public static <T> void assertSameElements(T[] collection, T... expected) {
        assertSameElements((Collection)Arrays.asList(collection), (Object[])expected);
    }

    public static <T> void assertSameElements(Collection<? extends T> collection, T... expected) {
        assertSameElements((Collection)collection, (Collection)Arrays.asList(expected));
    }

    public static <T> void assertSameElements(Collection<? extends T> collection, Collection<T> expected) {
        assertSameElements((String)null, collection, expected);
    }

    public static <T> void assertSameElements(String message, Collection<? extends T> collection, Collection<T> expected) {
        assertNotNull(collection);
        assertNotNull(expected);
        if(collection.size() != expected.size() || !(new HashSet(expected)).equals(new HashSet(collection))) {
            Assert.assertEquals(message, toString(expected, "\n"), toString(collection, "\n"));
            Assert.assertEquals(message, new HashSet(expected), new HashSet(collection));
        }

    }

    public static <T> void assertContainsOrdered(Collection<? extends T> collection, T... expected) {
        assertContainsOrdered(collection, (Collection)Arrays.asList(expected));
    }

    public static <T> void assertContainsOrdered(Collection<? extends T> collection, Collection<T> expected) {
        ArrayList copy = new ArrayList(collection);
        copy.retainAll(expected);
        assertOrderedEquals(toString(collection), copy, (Collection)expected);
    }

    public static <T> void assertContainsElements(Collection<? extends T> collection, T... expected) {
        assertContainsElements(collection, (Collection)Arrays.asList(expected));
    }

    public static <T> void assertContainsElements(Collection<? extends T> collection, Collection<T> expected) {
        ArrayList copy = new ArrayList(collection);
        copy.retainAll(expected);
        assertSameElements(toString(collection), copy, expected);
    }

    public static String toString(Object[] collection, String separator) {
        return toString((Collection)Arrays.asList(collection), separator);
    }

    public static <T> void assertDoesntContain(Collection<? extends T> collection, T... notExpected) {
        assertDoesntContain(collection, (Collection)Arrays.asList(notExpected));
    }

    public static <T> void assertDoesntContain(Collection<? extends T> collection, Collection<T> notExpected) {
        ArrayList expected = new ArrayList(collection);
        expected.removeAll(notExpected);
        assertSameElements((Collection)collection, (Collection)expected);
    }

    public static String toString(Collection<?> collection, String separator) {
        List list = ContainerUtil.map2List(collection, new Function() {
            public String fun(Object o) {
                return String.valueOf(o);
            }
        });
        Collections.sort(list);
        StringBuilder builder = new StringBuilder();
        boolean flag = false;

        for(Iterator var5 = list.iterator(); var5.hasNext(); flag = true) {
            String o = (String)var5.next();
            if(flag) {
                builder.append(separator);
            }

            builder.append(o);
        }

        return builder.toString();
    }

    public static <T> void assertOrderedCollection(Collection<? extends T> collection, Consumer... checkers) {
        Assert.assertNotNull(collection);
        if(collection.size() != checkers.length) {
            Assert.fail(toString(collection));
        }

        int i = 0;

        for(Iterator var3 = collection.iterator(); var3.hasNext(); ++i) {
            Object actual = var3.next();

            try {
                checkers[i].consume(actual);
            } catch (AssertionFailedError var6) {
                System.out.println(i + ": " + actual);
                throw var6;
            }
        }

    }

    public static <T> void assertUnorderedCollection(T[] collection, Consumer... checkers) {
        assertUnorderedCollection((Collection)Arrays.asList(collection), checkers);
    }

    public static <T> void assertUnorderedCollection(Collection<? extends T> collection, Consumer... checkers) {
        Assert.assertNotNull(collection);
        if(collection.size() != checkers.length) {
            Assert.fail(toString(collection));
        }

        HashSet checkerSet = new HashSet(Arrays.asList(checkers));
        int i = 0;
        Throwable lastError = null;

        for(Iterator var5 = collection.iterator(); var5.hasNext(); ++i) {
            Object actual = var5.next();
            boolean flag = true;

            Throwable error;
            for(Iterator var8 = checkerSet.iterator(); var8.hasNext(); lastError = error) {
                Consumer condition = (Consumer)var8.next();
                error = accepts(condition, actual);
                if(error == null) {
                    checkerSet.remove(condition);
                    flag = false;
                    break;
                }
            }

            if(flag) {
                lastError.printStackTrace();
                Assert.fail("Incorrect element(" + i + "): " + actual);
            }
        }

    }

    private static <T> Throwable accepts(Consumer<T> condition, T actual) {
        try {
            condition.consume(actual);
            return null;
        } catch (Throwable var3) {
            return var3;
        }
    }

    @Contract("null, _ -> fail")
    public static <T> T assertInstanceOf(Object o, Class<T> aClass) {
        Assert.assertNotNull("Expected instance of: " + aClass.getName() + " actual: " + null, o);
        Assert.assertTrue("Expected instance of: " + aClass.getName() + " actual: " + o.getClass().getName(), aClass.isInstance(o));
        return (T) o;
    }

    public static <T> T assertOneElement(Collection<T> collection) {
        Assert.assertNotNull(collection);
        Iterator iterator = collection.iterator();
        String toString = toString(collection);
        Assert.assertTrue(toString, iterator.hasNext());
        Object t = iterator.next();
        Assert.assertFalse(toString, iterator.hasNext());
        return (T) t;
    }

    public static <T> T assertOneElement(T[] ts) {
        Assert.assertNotNull(ts);
        Assert.assertEquals(Arrays.asList(ts).toString(), 1L, (long)ts.length);
        return ts[0];
    }

    @Contract("null, _ -> fail")
    public static <T> void assertOneOf(T value, T... values) {
        boolean found = false;
        Object[] var3 = values;
        int var4 = values.length;

        for(int var5 = 0; var5 < var4; ++var5) {
            Object v = var3[var5];
            if(value == v || value != null && value.equals(v)) {
                found = true;
            }
        }

        Assert.assertTrue(value + " should be equal to one of " + Arrays.toString(values), found);
    }

    public static void printThreadDump() {
        PerformanceWatcher.dumpThreadsToConsole("Thread dump:");
    }

    public static void assertEmpty(Object[] array) {
        assertOrderedEquals(array, new Object[0]);
    }

    public static void assertNotEmpty(Collection<?> collection) {
        if(collection != null) {
            assertTrue(!collection.isEmpty());
        }
    }

    public static void assertEmpty(Collection<?> collection) {
        assertEmpty(collection.toString(), collection);
    }

    public static void assertNullOrEmpty(Collection<?> collection) {
        if(collection != null) {
            assertEmpty((String)null, collection);
        }
    }

    public static void assertEmpty(String s) {
        assertTrue(s, StringUtil.isEmpty(s));
    }

    public static <T> void assertEmpty(String errorMsg, Collection<T> collection) {
        assertOrderedEquals(errorMsg, collection, (T[])(new Object[0]));
    }

    public static void assertSize(int expectedSize, Object[] array) {
        assertEquals(toString(Arrays.asList(array)), expectedSize, array.length);
    }

    public static void assertSize(int expectedSize, Collection<?> c) {
        assertEquals(toString(c), expectedSize, c.size());
    }

    protected <T extends Disposable> T disposeOnTearDown(T disposable) {
        Disposer.register(this.myTestRootDisposable, disposable);
        return disposable;
    }

    public static void assertSameLines(String expected, String actual) {
        String expectedText = StringUtil.convertLineSeparators(expected.trim());
        String actualText = StringUtil.convertLineSeparators(actual.trim());
        Assert.assertEquals(expectedText, actualText);
    }

    public static void assertExists(File file) {
        assertTrue("File should exist " + file, file.exists());
    }

    public static void assertDoesntExist(File file) {
        assertFalse("File should not exist " + file, file.exists());
    }

    protected String getTestName(boolean lowercaseFirstLetter) {
        return getTestName(this.getName(), lowercaseFirstLetter);
    }

    public static String getTestName(String name, boolean lowercaseFirstLetter) {
        return name == null? "": KtPlatformTestUtil.getTestName(name, lowercaseFirstLetter);
    }

    /** @deprecated */
    public static String lowercaseFirstLetter(String name, boolean lowercaseFirstLetter) {
        return KtPlatformTestUtil.lowercaseFirstLetter(name, lowercaseFirstLetter);
    }

    /** @deprecated */
    public static boolean isAllUppercaseName(String name) {
        return KtPlatformTestUtil.isAllUppercaseName(name);
    }

    protected String getTestDirectoryName() {
        String testName = this.getTestName(true);
        return testName.replaceAll("_.*", "");
    }

    public static void assertSameLinesWithFile(String filePath, String actualText) {
        assertSameLinesWithFile(filePath, actualText, true);
    }

    public static void assertSameLinesWithFile(String filePath, String actualText, boolean trimBeforeComparing) {
        String fileText;
        try {
            if(OVERWRITE_TESTDATA) {
                VfsTestUtil.overwriteTestData(filePath, actualText);
                System.out.println("File " + filePath + " created.");
            }

            fileText = FileUtil.loadFile(new File(filePath), CharsetToolkit.UTF8_CHARSET);
        } catch (FileNotFoundException var6) {
            VfsTestUtil.overwriteTestData(filePath, actualText);
            throw new AssertionFailedError("No output text found. File " + filePath + " created.");
        } catch (IOException var7) {
            throw new RuntimeException(var7);
        }

        String expected = StringUtil.convertLineSeparators(trimBeforeComparing?fileText.trim():fileText);
        String actual = StringUtil.convertLineSeparators(trimBeforeComparing?actualText.trim():actualText);
        if(!Comparing.equal(expected, actual)) {
            throw new FileComparisonFailure((String)null, expected, actual, filePath);
        }
    }

    public static void clearFields(Object test) throws IllegalAccessException {
        for(Class aClass = test.getClass(); aClass != null; aClass = aClass.getSuperclass()) {
            clearDeclaredFields(test, aClass);
        }

    }

    public static void clearDeclaredFields(Object test, Class aClass) throws IllegalAccessException {
        if(aClass != null) {
            Field[] var2 = aClass.getDeclaredFields();
            int var3 = var2.length;

            for(int var4 = 0; var4 < var3; ++var4) {
                Field field = var2[var4];
                String name = field.getDeclaringClass().getName();
                if(!name.startsWith("junit.framework.") && !name.startsWith("com.intellij.testFramework.")) {
                    int modifiers = field.getModifiers();
                    if((modifiers & 16) == 0 && (modifiers & 8) == 0 && !field.getType().isPrimitive()) {
                        field.setAccessible(true);
                        field.set(test, (Object)null);
                    }
                }
            }

        }
    }

    protected static void checkSettingsEqual(CodeStyleSettings expected, CodeStyleSettings settings, String message) throws Exception {
        if(expected != null && settings != null) {
            Element oldS = new Element("temp");
            expected.writeExternal(oldS);
            Element newS = new Element("temp");
            settings.writeExternal(newS);
            String newString = JDOMUtil.writeElement(newS, "\n");
            String oldString = JDOMUtil.writeElement(oldS, "\n");
            Assert.assertEquals(message, oldString, newString);
        }
    }

    public boolean isPerformanceTest() {
        String name = this.getName();
        return name != null && name.contains("Performance") || this.getClass().getName().contains("Performance");
    }

    protected static void checkAllTimersAreDisposed(@NotNull List<Throwable> exceptions) {
        Field firstTimerF;
        Object timerQueue;
        Object timer;
        try {
            Class text = Class.forName("javax.swing.TimerQueue");
            Method t = text.getDeclaredMethod("sharedInstance", new Class[0]);
            t.setAccessible(true);
            firstTimerF = ReflectionUtil.getDeclaredField(text, "firstTimer");
            timerQueue = t.invoke((Object)null, new Object[0]);
            if(firstTimerF == null) {
                DelayQueue delayQueue = (DelayQueue)ReflectionUtil.getField(text, timerQueue, DelayQueue.class, "queue");
                timer = delayQueue.peek();
            } else {
                firstTimerF.setAccessible(true);
                timer = firstTimerF.get(timerQueue);
            }
        } catch (Throwable var10) {
            exceptions.add(var10);
            return;
        }

        if(timer != null) {
            if(firstTimerF != null) {
                ReflectionUtil.resetField(timerQueue, firstTimerF);
            }

            String var11 = "";
            if(timer instanceof Delayed) {
                long var12 = ((Delayed)timer).getDelay(TimeUnit.MILLISECONDS);
                var11 = "(delayed for " + var12 + "ms)";
                Method getTimer = ReflectionUtil.getDeclaredMethod(timer.getClass(), "getTimer", new Class[0]);
                getTimer.setAccessible(true);

                try {
                    timer = getTimer.invoke(timer, new Object[0]);
                } catch (Exception var9) {
                    exceptions.add(var9);
                    return;
                }
            }

            javax.swing.Timer var13 = (javax.swing.Timer)timer;
            var11 = "Timer (listeners: " + Arrays.asList(var13.getActionListeners()) + ") " + var11;
            exceptions.add(new AssertionFailedError("Not disposed Timer: " + var11 + "; queue:" + timerQueue));
        }

    }

    protected void assertException(AbstractExceptionCase exceptionCase) throws Throwable {
        this.assertException(exceptionCase, (String)null);
    }

    protected void assertException(AbstractExceptionCase exceptionCase, @Nullable String expectedErrorMsg) throws Throwable {
        assertExceptionOccurred(true, exceptionCase, expectedErrorMsg);
    }

    protected void assertNoException(AbstractExceptionCase exceptionCase) throws Throwable {
        assertExceptionOccurred(false, exceptionCase, (String)null);
    }

    protected void assertNoThrowable(Runnable closure) {
        String throwableName = null;

        try {
            closure.run();
        } catch (Throwable var4) {
            throwableName = var4.getClass().getName();
        }

        assertNull(throwableName);
    }

    private static void assertExceptionOccurred(boolean shouldOccur, AbstractExceptionCase exceptionCase, String expectedErrorMsg) throws Throwable {
        boolean wasThrown = false;

        try {
            exceptionCase.tryClosure();
        } catch (Throwable var9) {
            if(shouldOccur) {
                wasThrown = true;
                String errorMessage = exceptionCase.getAssertionErrorMessage();
                assertEquals(errorMessage, exceptionCase.getExpectedExceptionClass(), var9.getClass());
                if(expectedErrorMsg != null) {
                    assertEquals("Compare error messages", expectedErrorMsg, var9.getMessage());
                }
            } else {
                if(!exceptionCase.getExpectedExceptionClass().equals(var9.getClass())) {
                    throw var9;
                }

                wasThrown = true;
                System.out.println("");
                var9.printStackTrace(System.out);
                fail("Exception isn\'t expected here. Exception message: " + var9.getMessage());
            }
        } finally {
            if(shouldOccur && !wasThrown) {
                fail(exceptionCase.getAssertionErrorMessage());
            }

        }

    }

    protected boolean annotatedWith(@NotNull Class annotationClass) {
        Class aClass = this.getClass();
        String methodName = "test" + this.getTestName(false);

        for(boolean methodChecked = false; aClass != null && aClass != Object.class; aClass = aClass.getSuperclass()) {
            if(aClass.getAnnotation(annotationClass) != null) {
                return true;
            }

            if(!methodChecked) {
                Method method = ReflectionUtil.getDeclaredMethod(aClass, methodName, new Class[0]);
                if(method != null) {
                    if(method.getAnnotation(annotationClass) != null) {
                        return true;
                    }

                    methodChecked = true;
                }
            }
        }

        return false;
    }

    protected String getHomePath() {
        return PathManager.getHomePath().replace(File.separatorChar, '/');
    }

    protected static boolean isInHeadlessEnvironment() {
        return GraphicsEnvironment.isHeadless();
    }

    public static void refreshRecursively(@NotNull VirtualFile file) {
        VfsUtilCore.visitChildrenRecursively(file, new VirtualFileVisitor(new VirtualFileVisitor.Option[0]) {
            public boolean visitFile(@NotNull VirtualFile file) {
                file.getChildren();
                return true;
            }
        });
        file.refresh(false, true);
    }

    //@NotNull
    //public static Test filteredSuite(@RegExp String regexp, @NotNull Test test) {
    //    final Pattern pattern = Pattern.compile(regexp);
    //    final TestSuite testSuite = new TestSuite();
    //    (new Processor() {
    //        public boolean process(Test test) {
    //            if(test instanceof TestSuite) {
    //                int i = 0;
    //
    //                for(int len = ((TestSuite)test).testCount(); i < len; ++i) {
    //                    this.process(((TestSuite)test).testAt(i));
    //                }
    //            } else if(pattern.matcher(test.toString()).find()) {
    //                testSuite.addTest(test);
    //            }
    //
    //            return false;
    //        }
    //    }).process(test);
    //    return testSuite;
    //}

    @Nullable
    public static VirtualFile refreshAndFindFile(@NotNull final File file) {
        return (VirtualFile)UIUtil.invokeAndWaitIfNeeded(new Computable() {
            public VirtualFile compute() {
                return LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);
            }
        });
    }

    public static <E extends Exception> void invokeAndWaitIfNeeded(@NotNull final ThrowableRunnable<E> runnable) throws Exception {
        if(SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            final Ref ref = Ref.create();
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    try {
                        runnable.run();
                    } catch (Exception var2) {
                        ref.set(var2);
                    }

                }
            });
            if(!ref.isNull()) {
                throw (Exception)ref.get();
            }
        }

    }

    static {
        System.setProperty("apple.awt.UIElement", "true");

        try {
            //CodeInsightSettings aClass = new CodeInsightSettings();
            Element files = new Element("temp");
            //aClass.writeExternal(files);
            DEFAULT_SETTINGS_EXTERNALIZED = JDOMUtil.writeElement(files, "\n");
        } catch (Exception var3) {
            throw new RuntimeException(var3);
        }

        Class aClass1;
        try {
            aClass1 = Class.forName("java.io.DeleteOnExitHook");
        } catch (Exception var2) {
            throw new RuntimeException(var2);
        }

        Set files1 = (Set)ReflectionUtil.getStaticFieldValue(aClass1, Set.class, "files");
        DELETE_ON_EXIT_HOOK_CLASS = aClass1;
        DELETE_ON_EXIT_HOOK_DOT_FILES = files1;
    }
}
