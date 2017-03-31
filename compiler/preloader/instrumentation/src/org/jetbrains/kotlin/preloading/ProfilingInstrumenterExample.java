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

package org.jetbrains.kotlin.preloading;

import org.jetbrains.kotlin.preloading.instrumentation.InterceptionInstrumenterAdaptor;
import org.jetbrains.kotlin.preloading.instrumentation.annotations.ClassName;
import org.jetbrains.kotlin.preloading.instrumentation.annotations.MethodDesc;
import org.jetbrains.kotlin.preloading.instrumentation.annotations.MethodInterceptor;
import org.jetbrains.kotlin.preloading.instrumentation.annotations.MethodName;

import java.io.PrintStream;
import java.util.*;

@SuppressWarnings("UnusedDeclaration")
public class ProfilingInstrumenterExample extends InterceptionInstrumenterAdaptor {

    // How many times are visit* methods of visitors called?
    @MethodInterceptor(className = ".*Visitor.*", methodName = "visit.*", methodDesc = ".*", allowMultipleMatches = true)
    public static final Object a = new InvocationCount();

    public static class InvocationCount {
        private int count = 0;

        public void enter() {
            // This method is called upon entering a visit* method
            count++;
        }

        public void dump(PrintStream out) {
            // This method is called upon program termination
            out.println("Invocation count: " + count);
        }
    }

    // How much time do we spend in equals() methods of all classes inside package org
    // NOTE: this works only on methods actually DECLARED in these classes
    // This also logs names of actually instrumented methods to console
    @MethodInterceptor(className = "org/.*", methodName = "equals", methodDesc = "\\(Ljava/lang/Object;\\)Z", logInterceptions = true)
    public static final Object b = new TotalTime();

    public static class TotalTime {
        private long time = 0;
        private long start = 0;
        private boolean started = false;

        public void enter() {
            if (!started) {
                start = System.nanoTime();
                started = true;
            }
        }

        public void exit() {
            if (started) {
                time += System.nanoTime() - start;
                started = false;
            }
        }

        public void dump(PrintStream out) {
            out.printf("Total time: %.3fs\n", (time / 1e9));
        }
    }

    // Collect all strings that were capitalized using StringUtil, and dump its instrumented byte code
    @MethodInterceptor(className = "com/intellij/openapi/util/text/StringUtil",
                       methodName = "capitalize",
                       methodDesc = "\\(Ljava/lang/String;\\).*",
                       dumpByteCode = true)
    public static Object c = new CollectFirstArguments();

    public static class CollectFirstArguments {
        private final List<Object> arguments = new ArrayList<>(30000);

        public void enter(Object arg) {
            arguments.add(arg);
        }

        public void dump(PrintStream out) {
            out.println("Different values: " + new HashSet<>(arguments).size());
        }
    }

    // What methods that have a long parameter followed by some object parameter are actually called
    @MethodInterceptor(className = ".*",
                       methodName = ".*",
                       methodDesc = "\\(.*JL.*?\\).*",
                       allowMultipleMatches = true)
    public static Object d = new MethodCollector();

    public static class MethodCollector {
        private final Collection<String> collected = new LinkedHashSet<>();

        public void enter(@ClassName String className, @MethodName String name, @MethodDesc String desc) {
            collected.add(className + "." + name + desc);
        }

        public void dump(PrintStream out) {
            for (String s : collected) {
                out.println(s);
            }
        }
    }

    // What integers are passed as first arguments to any methods?
    @MethodInterceptor(className = ".*",
                       methodName = ".*",
                       methodDesc = "\\(.+\\).*",
                       allowMultipleMatches = true)
    public static Object e = new FirstArgumentCollector() {
        @Override
        protected boolean accept(Object arg) {
            return arg instanceof Integer;
        }
    };

    public static abstract class FirstArgumentCollector {
        private final Collection<Object> collected = new HashSet<>();

        protected abstract boolean accept(Object arg);

        public void enter(Object arg) {
            if (accept(arg)) {
                collected.add(arg);
            }
        }

        public void dump(PrintStream out) {
            out.println("Arguments: " + collected.size());
        }
    }
}
