/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.junit.internal.MethodSorter;
import org.junit.internal.runners.JUnit38ClassRunner;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.manipulation.*;
import org.junit.runner.notification.RunNotifier;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.jetbrains.kotlin.test.JUnit3RunnerWithInners.isTestMethod;

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
class JUnit3RunnerWithInnersForGradle extends Runner implements Filterable, Sortable {
    private final JUnit38ClassRunner delegateRunner;

    public JUnit3RunnerWithInnersForGradle(Class<?> klass) {
        super();

        String className = klass.getName();

        Test test = new TestSuite(klass.asSubclass(TestCase.class));
        if (!hasOwnTestMethods(klass)) {
            for (Class<?> declaredClass : klass.getDeclaredClasses()) {
                if (TestCase.class.isAssignableFrom(declaredClass)) {
                    test = new JUnit3RunnerWithInners.FakeEmptyClassTest(klass);
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
            @SuppressWarnings("unchecked") List<Object> includePatterns =
                    (ArrayList<Object>) includePatternsField.get(testSelectionMatcher);

            if (includePatterns.size() != 1) {
                return null;
            }

            Object patternStorage = includePatterns.get(0);

            Pattern pattern;
            if (patternStorage instanceof Pattern) {
                pattern = (Pattern) patternStorage;
            } else {
                try {
                    // TestSelectionMatcher.TestPattern is used since Gradle 4.7
                    Field patternField = patternStorage.getClass().getDeclaredField("pattern");
                    patternField.setAccessible(true);
                    pattern = (Pattern) patternField.get(patternStorage);
                }
                catch (NoSuchFieldException exception) {
                    return null;
                }
            }

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