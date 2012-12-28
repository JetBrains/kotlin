/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Stack;
import java.util.concurrent.locks.ReentrantLock;

public class Profiler {
    // The stack is synchronized here: this is intentional
    private static final ThreadLocal<Stack<Profiler>> PROFILERS = new ThreadLocal<Stack<Profiler>>() {
        @Override
        protected Stack<Profiler> initialValue() {
            return new Stack<Profiler>();
        }
    };

    private static final ReentrantLock OUT_LOCK = new ReentrantLock();

    @NotNull
    public static Profiler create(@NotNull String name) {
        return create(name, null);
    }

    @NotNull
    public static Profiler create(@NotNull String name, @Nullable PrintStream out) {
        Profiler profiler = new Profiler(name, out);
        PROFILERS.get().push(profiler);
        return profiler;
    }

    public static Profiler getFromContext() {
        Stack<Profiler> profilers = PROFILERS.get();
        if (profilers.isEmpty()) {
            throw new UnsupportedOperationException();
        }
        return profilers.peek();
    }

    private final String name;
    private final PrintStream out;
    private long start;
    private StackTraceElement[] stackTrace;
    private boolean mute;

    private Profiler(@NotNull String name, @Nullable PrintStream out) {
        this.name = name;
        this.out = out == null ? System.out : out;
    }

    public Profiler recordStackTrace(int depth) {
        return recordStackTrace(1 /*skipping this frame*/, depth);
    }

    public Profiler recordStackTrace(int skip, int depth) {
        StackTraceElement[] trace = new Throwable().getStackTrace();

        int from = 1 + skip;
        if (from >= trace.length) return this;

        int to;
        if (depth == -1) {
            to = trace.length;
        }
        else {
            to = Math.min(skip + depth + 1, trace.length);
        }

        stackTrace = Arrays.copyOfRange(trace, from, to);
        return this;
    }

    public Profiler resetStackTrace() {
        stackTrace = null;
        return this;
    }

    public Profiler printStackTrace() {
        if (stackTrace != null) {
            OUT_LOCK.lock();
            try {
                for (StackTraceElement element : stackTrace) {
                    println("\tat ", element);
                }
            }
            finally {
                OUT_LOCK.unlock();
            }
        }
        return this;
    }

    public Profiler printEntering() {
        println("Entering ", name);
        return this;
    }

    public Profiler start() {
        start = System.nanoTime();
        return this;
    }

    public Profiler end() {
        long delta = System.nanoTime() - start;

        OUT_LOCK.lock();
        try {
            println(name, " took ", format(delta));
            printStackTrace();
        }
        finally {
            OUT_LOCK.unlock();
        }

        return this;
    }

    public Profiler mute() {
        mute = true;
        return this;
    }

    public Profiler unmute() {
        mute = false;
        return this;
    }

    public Profiler println(Object message) {
        if (!mute) {
            out.println(message);
        }
        return this;
    }

    public Profiler println(Object a, Object b) {
        if (!mute) {
            OUT_LOCK.lock();
            try {
                out.print(a);
                out.println(b);
            }
            finally {
                OUT_LOCK.unlock();
            }
        }
        return this;
    }

    public Profiler println(Object a, Object b, Object c) {
        if (!mute) {
            OUT_LOCK.lock();
            try {
                out.print(a);
                out.print(b);
                out.println(c);
            }
            finally {
                OUT_LOCK.unlock();
            }
        }
        return this;
    }

    public Profiler println(Object a, Object b, Object c, Object... rest) {
        if (!mute) {
            OUT_LOCK.lock();
            try {
                out.print(a);
                out.print(b);
                out.print(c);
                for (Object o : rest) {
                    out.print(o);
                }
                out.println();
            }
            finally {
                OUT_LOCK.unlock();
            }
        }
        return this;
    }

    private static String format(long delta) {
        return String.format("%.3fs", delta / 1e9);
    }
}
