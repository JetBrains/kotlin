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
import junit.framework.TestSuite;
import org.junit.internal.MethodSorter;
import org.junit.internal.runners.JUnit38ClassRunner;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.manipulation.*;
import org.junit.runner.notification.RunNotifier;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.jetbrains.kotlin.test.JUnit3RunnerWithInners.isTestMethod;

/**
 * This runner runs class with all inners test classes, but monitors situation when those classes are planned to be executed
 * with IDEA package test runner.
 */
public class JUnit3RunnerWithInnersForJPS extends Runner implements Filterable, Sortable {
    private static final Set<Class> requestedRunners = new HashSet<>();

    private JUnit38ClassRunner delegateRunner;
    private final Class<?> klass;
    private boolean isFakeTest = false;

    public JUnit3RunnerWithInnersForJPS(Class<?> klass) {
        this.klass = klass;
        requestedRunners.add(klass);
        ensureCompilerXmlExists();
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
        initialize();
        delegateRunner.filter(filter);
    }

    @Override
    public void sort(Sorter sorter) {
        initialize();
        delegateRunner.sort(sorter);
    }

    protected void initialize() {
        if (delegateRunner != null) return;
        delegateRunner = new JUnit38ClassRunner(getCollectedTests());
    }

    /**
     * compiler.xml needs to be in both compiler & ide module for tests execution.
     * To avoid file duplication copy it to the out dir idea module before test execution.
     */
    private static void ensureCompilerXmlExists() {
        String compilerXmlSourcePath = "compiler/cli/cli-common/resources/META-INF/extensions/compiler.xml";

        String jpsTargetDirectory = "out/production/kotlin.idea.main";
        String pillTargetDirectory = "out/production/idea.main";

        String baseDir = Files.exists(Paths.get(jpsTargetDirectory)) ? jpsTargetDirectory : pillTargetDirectory;
        String compilerXmlTargetPath = baseDir + "/META-INF/extensions/compiler.xml";

        try {
            Path targetPath = Paths.get(compilerXmlTargetPath);
            Files.createDirectories(targetPath.getParent());
            Files.copy(Paths.get(compilerXmlSourcePath),targetPath, REPLACE_EXISTING);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Test getCollectedTests() {
        List<Class> innerClasses = collectDeclaredClasses(klass, false);
        Set<Class> unprocessedInnerClasses = unprocessedClasses(innerClasses);

        if (unprocessedInnerClasses.isEmpty()) {
            if (!innerClasses.isEmpty() && !hasTestMethods(klass)) {
                isFakeTest = true;
                return new JUnit3RunnerWithInners.FakeEmptyClassTest(klass);
            }
            else {
                return new TestSuite(klass.asSubclass(TestCase.class));
            }
        }
        else if (unprocessedInnerClasses.size() == innerClasses.size()) {
            return createTreeTestSuite(klass);
        }
        else {
            return new TestSuite(klass.asSubclass(TestCase.class));
        }
    }

    private static Test createTreeTestSuite(Class root) {
        Set<Class> classes = new LinkedHashSet<>(collectDeclaredClasses(root, true));
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
            if (!requestedRunners.contains(aClass)) {
                result.add(aClass);
            }
        }

        return result;
    }

    private static List<Class> collectDeclaredClasses(Class klass, boolean withItself) {
        List<Class> result = new ArrayList<>();
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
}