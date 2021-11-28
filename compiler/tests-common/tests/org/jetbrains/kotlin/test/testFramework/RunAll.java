/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.testFramework;

import com.intellij.util.SmartList;
import com.intellij.util.ThrowableRunnable;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.lang.CompoundRuntimeException;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RunAll {
    private final List<? extends ThrowableRunnable<?>> myActions;

    @SafeVarargs
    public RunAll(ThrowableRunnable<Throwable> ... actions) {
        this(Arrays.asList(actions));
    }

    public RunAll(@NotNull List<? extends ThrowableRunnable<?>> actions) {
        myActions = actions;
    }

    @SafeVarargs
    @Contract(pure=true)
    public final RunAll append(ThrowableRunnable<Throwable> ... actions) {
        return new RunAll(ContainerUtil.concat(myActions, actions.length == 1 ? Collections.singletonList(actions[0]) : Arrays.asList(actions)));
    }

    public void run() {
        run(Collections.emptyList());
    }

    public void run(@NotNull List<? extends Throwable> suppressedExceptions) {
        CompoundRuntimeException.throwIfNotEmpty(ContainerUtil.concat(suppressedExceptions, collectExceptions(myActions)));
    }

    private static @NotNull List<Throwable> collectExceptions(@NotNull List<? extends ThrowableRunnable<?>> actions) {
        List<Throwable> result = null;
        for (ThrowableRunnable<?> action : actions) {
            try {
                action.run();
            }
            catch (CompoundRuntimeException e) {
                if (result == null) {
                    result = new ArrayList<>();
                }
                result.addAll(e.getExceptions());
            }
            catch (Throwable e) {
                if (result == null) {
                    result = new SmartList<>();
                }
                result.add(e);
            }
        }
        return ContainerUtil.notNullize(result);
    }
}
