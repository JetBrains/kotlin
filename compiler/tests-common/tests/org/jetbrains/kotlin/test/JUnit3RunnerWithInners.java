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
import junit.framework.TestCase;
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
import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * This runner executes own tests and bypass Gradle filtering for inner classes execution.
 * Together with the hack in `tasks.kt - fun Project.projectTest()` that adds inner class files to processing,
 * this allows running tests in inner classes when parent class pattern is used.
 *
 * This class also suppress "No tests found in class" warning when inner test classes are present.
 *
 * Previous implementation that was building test suite with test cases for inner classes automatically, produced unstable test names
 * on TeamCity. Names were different when inner test is executed as top class or part of the other parent class.
 */
public class JUnit3RunnerWithInners extends Runner implements Filterable, Sortable {
    private final JUnit38ClassRunner delegateRunner;

    private static class FakeEmptyClassTest implements Test, Filterable {
        private final String className;

        FakeEmptyClassTest(Class<?> klass) {
            this.className = klass.getName();
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
            return "Empty class with inners for " + className;
        }

        @Override
        public void filter(Filter filter) throws NoTestsRemainException {
            throw new NoTestsRemainException();
        }
    }

    public JUnit3RunnerWithInners(Class<?> klass) {
        super();

        String className = klass.getName();

        Test test = new TestSuite(klass.asSubclass(TestCase.class));
        if (!hasOwnTestMethods(klass)) {
            for (Class<?> declaredClass : klass.getDeclaredClasses()) {
                if (TestCase.class.isAssignableFrom(declaredClass)) {
                    test = new FakeEmptyClassTest(klass);
                    break;
                }
            }
        }

        delegateRunner = new JUnit38ClassRunner(test) {
            @Override public void filter(Filter filter) throws NoTestsRemainException {
                String classPatternString = getGradleClassPattern(filter);

                if (classPatternString != null) {
                    if (Pattern.compile(classPatternString + "\\$.*").matcher(className).matches()) {
                        return;
                    }
                }

                super.filter(filter);
            }
        };
    }

    @Override
    public void run(RunNotifier notifier) {
        delegateRunner.run(notifier);
    }

    @Override
    public Description getDescription() {
        return delegateRunner.getDescription();
    }

    @Override
    public void filter(Filter filter) throws NoTestsRemainException {
        delegateRunner.filter(filter);
    }

    @Override
    public void sort(Sorter sorter) {
        delegateRunner.sort(sorter);
    }

    private static boolean hasOwnTestMethods(Class klass) {
        for (Method each : MethodSorter.getDeclaredMethods(klass)) {
            if (isTestMethod(each)) return true;
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
            if (!"org.gradle.api.internal.tasks.testing.junit.JUnitTestClassExecutor$MethodNameFilter".equals(filterClass.getName())) {
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
        }
        catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}