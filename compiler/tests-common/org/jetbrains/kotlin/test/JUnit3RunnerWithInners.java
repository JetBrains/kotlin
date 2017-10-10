/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.test;

import junit.framework.Test;
import junit.framework.TestResult;
import junit.framework.TestSuite;
import org.junit.internal.MethodSorter;
import org.junit.internal.runners.JUnit38ClassRunner;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.manipulation.*;
import org.junit.runner.notification.RunNotifier;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.regex.Pattern;

/**
 * This runner runs class with all inners test classes, but monitors situation when
 * inner classes are already processed.
 */
public class JUnit3RunnerWithInners extends Runner implements Filterable, Sortable {
    private static final Set<Class> processedClasses = new HashSet<>();

    private JUnit38ClassRunner delegateRunner;
    private final Class<?> klass;
    private boolean isFakeTest = false;

    private static class FakeEmptyClassTest implements Test, Filterable {
        private final String klassName;

        FakeEmptyClassTest(Class<?> klass) {
            this.klassName = klass.getName();
        }

        @Override
        public int countTestCases() {
            return 0;
        }

        @Override
        public void run(TestResult result) {
            result.startTest(this);
            result.endTest(this);
        }

        @Override
        public String toString() {
            return "Empty class with inners for " + klassName;
        }

        @Override
        public void filter(Filter filter) throws NoTestsRemainException {
            throw new NoTestsRemainException();
        }
    }

    public JUnit3RunnerWithInners(Class<?> klass) {
        this.klass = klass;
    }

    @Override
    public void run(RunNotifier notifier) {
        initialize();
        delegateRunner.run(notifier);
    }

    @Override
    public Description getDescription() {
        initialize();
        return isFakeTest ? Description.EMPTY : delegateRunner.getDescription();
    }

    @Override
    public void filter(Filter filter) throws NoTestsRemainException {
        delegateRunner.filter(filter);
    }

    @Override
    public void sort(Sorter sorter) {
        initialize();
        delegateRunner.sort(sorter);
    }

    private void initialize() {
        if (delegateRunner != null) return;
        Test collectedTests = getCollectedTests();

        delegateRunner = new JUnit38ClassRunner(collectedTests) {
            @Override public void filter(Filter filter) throws NoTestsRemainException {
                String classDescription = collectedTests.toString();
                String classPatternString = getGradleClassPattern(filter);

                if (classPatternString != null) {
                    if (Pattern.compile(classPatternString + "\\$.*").matcher(classDescription).matches()) {
                        return;
                    }
                }

                super.filter(filter);
            }
        };
    }

    private Test getCollectedTests() {
        if (processedClasses.contains(klass)) {
            isFakeTest = true;
            return new FakeEmptyClassTest(klass);
        }

        Set<Class> classes = collectDeclaredClasses(klass, true);
        Set<Class> unprocessedClasses = unprocessedClasses(classes);
        processedClasses.addAll(unprocessedClasses);

        return createTreeTestSuite(klass, unprocessedClasses);
    }

    private static Test createTreeTestSuite(Class root, Set<Class> classes) {
        Map<Class, TestSuite> classSuites = new HashMap<>();

        for (Class aClass : classes) {
            classSuites.put(aClass, hasTestMethods(aClass) ? new TestSuite(aClass) : new TestSuite(aClass.getCanonicalName()));
        }

        for (Class aClass : classes) {
            if (aClass.getEnclosingClass() != null && classes.contains(aClass.getEnclosingClass())) {
                classSuites.get(aClass.getEnclosingClass()).addTest(classSuites.get(aClass));
            }
        }

        return classSuites.get(root);
    }

    private static Set<Class> unprocessedClasses(Collection<Class> classes) {
        Set<Class> result = new LinkedHashSet<>();
        for (Class aClass : classes) {
            if (!processedClasses.contains(aClass)) {
                result.add(aClass);
            }
        }

        return result;
    }

    private static Set<Class> collectDeclaredClasses(Class klass, boolean withItself) {
        Set<Class> result = new HashSet<>();
        if (withItself) {
            result.add(klass);
        }

        for (Class aClass : klass.getDeclaredClasses()) {
            result.addAll(collectDeclaredClasses(aClass, true));
        }

        return result;
    }

    private static boolean hasTestMethods(Class klass) {
        for (Class currentClass = klass; Test.class.isAssignableFrom(currentClass); currentClass = currentClass.getSuperclass()) {
            for (Method each : MethodSorter.getDeclaredMethods(currentClass)) {
                if (isTestMethod(each)) return true;
            }
        }

        return false;
    }

    private static boolean isTestMethod(Method method) {
        return method.getParameterTypes().length == 0 &&
               method.getName().startsWith("test") &&
               method.getReturnType().equals(Void.TYPE) &&
               Modifier.isPublic(method.getModifiers());
    }

    private static String getGradleClassPattern(Filter filter) {
        try {
            Class<? extends Filter> filterClass = filter.getClass();
            if (!"org.gradle.api.internal.tasks.testing.junit.JUnitTestClassExecuter$MethodNameFilter".equals(filterClass.getName())) {
                return null;
            }

            Field matcherField = filterClass.getDeclaredField("matcher");
            matcherField.setAccessible(true);
            Object testSelectionMatcher = matcherField.get(filter);
            Class<?> testSelectionMatcherClass = testSelectionMatcher.getClass();
            if (!"org.gradle.api.internal.tasks.testing.filter.TestSelectionMatcher".equals(testSelectionMatcherClass.getName())) {
                return null;
            }

            Field includePatternsField;
            try {
                includePatternsField = testSelectionMatcherClass.getDeclaredField("includePatterns");
            }
            catch (NoSuchFieldException exception) {
                includePatternsField = testSelectionMatcherClass.getDeclaredField("buildScriptIncludePatterns");
            }

            includePatternsField.setAccessible(true);
            @SuppressWarnings("unchecked") ArrayList<Pattern> includePatterns =
                    (ArrayList<Pattern>) includePatternsField.get(testSelectionMatcher);

            if (includePatterns.size() != 1) {
                return null;
            }

            Pattern pattern = includePatterns.get(0);
            String patternStr = pattern.pattern();

            if (patternStr.endsWith("*")) {
                return null;
            }

            return patternStr;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}