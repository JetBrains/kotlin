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
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.manipulation.*;
import org.junit.runner.notification.RunNotifier;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Runner that is responsible for executing tests including test methods from all inner classes.
 * Works differently for Gradle and JPS. Default is Gradle for now.
 */
public class JUnit3RunnerWithInners extends Runner implements Filterable, Sortable {
    private final Runner delegateRunner;

    public JUnit3RunnerWithInners(Class<?> klass) {
        super();

        if ("true".equals(System.getProperty("use.jps"))) {
            delegateRunner = new JUnit3RunnerWithInnersForJPS(klass);
        }
        else {
            delegateRunner = new JUnit3RunnerWithInnersForGradle(klass);
        }
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
        ((Filterable)delegateRunner).filter(filter);
    }

    @Override
    public void sort(Sorter sorter) {
        ((Sortable)delegateRunner).sort(sorter);
    }

    static class FakeEmptyClassTest implements Test, Filterable {
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

    static boolean isTestMethod(Method method) {
        return method.getParameterTypes().length == 0 &&
               method.getName().startsWith("test") &&
               method.getReturnType().equals(Void.TYPE) &&
               Modifier.isPublic(method.getModifiers());
    }
}