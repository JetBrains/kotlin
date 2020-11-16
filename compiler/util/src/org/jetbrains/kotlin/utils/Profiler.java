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

package org.jetbrains.kotlin.utils;

import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;

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
        //noinspection UseOfSystemOutOrSystemErr
        return create(name, System.out);
    }

    @NotNull
    public static Profiler create(@NotNull String name, @NotNull PrintStream out) {
        return create(name, new PrintingLogger(out));
    }

    @NotNull
    public static Profiler create(@NotNull String name, @NotNull Logger log) {
        Profiler profiler = new Profiler(name, log);
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
    private final Logger log;
    private long start = Long.MAX_VALUE;
    private long cumulative = 0;
    private boolean paused = true;
    private StackTraceElement[] stackTrace;
    private boolean mute;
    private String formatString;

    private Profiler(@NotNull String name, @NotNull Logger log) {
        this.name = name;
        this.log = log;
        setPrintAccuracy(3);
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
        if (stackTrace != null && log.isDebugEnabled()) {
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

    public Profiler printThreadName() {
        println(Thread.currentThread().getName() + " ", name);
        return this;
    }

    public Profiler start() {
        if (paused) {
            start = System.nanoTime();
            paused = false;
        }
        return this;
    }

    public Profiler end() {
        long result = cumulative;
        if (!paused) {
            result += System.nanoTime() - start;
        }
        paused = true;
        cumulative = 0;

        if (!mute && log.isDebugEnabled()) {
            OUT_LOCK.lock();
            try {
                println(name, " took ", format(result));
                printStackTrace();
            }
            finally {
                OUT_LOCK.unlock();
            }
        }

        return this;
    }

    public Profiler pause() {
        if (!paused) {
            cumulative += System.nanoTime() - start;
            paused = true;
        }
        return this;
    }

    public long getCumulative() {
        return cumulative;
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
        if (!mute && log.isDebugEnabled()) {
            log.debug(String.valueOf(message));
        }
        return this;
    }

    public Profiler println(Object a, Object b) {
        if (!mute && log.isDebugEnabled()) {
            OUT_LOCK.lock();
            try {
                log.debug(String.valueOf(a) + b);
            }
            finally {
                OUT_LOCK.unlock();
            }
        }
        return this;
    }

    public Profiler println(Object a, Object b, Object c) {
        if (!mute && log.isDebugEnabled()) {
            OUT_LOCK.lock();
            try {
                log.debug(String.valueOf(a) + b + c);
            }
            finally {
                OUT_LOCK.unlock();
            }
        }
        return this;
    }

    public Profiler println(Object a, Object b, Object c, Object... rest) {
        if (!mute && log.isDebugEnabled()) {
            OUT_LOCK.lock();
            try {
                StringBuilder sb = new StringBuilder();
                sb.append(a);
                sb.append(b);
                sb.append(c);
                for (Object o : rest) {
                    sb.append(o);
                }
                log.debug(sb.toString());
            }
            finally {
                OUT_LOCK.unlock();
            }
        }
        return this;
    }

    public Profiler setPrintAccuracy(int accuracy) {
        formatString = "%." + accuracy + "fs";
        return this;
    }

    private String format(long delta) {
        return String.format(formatString, delta / 1e9);
    }
}
