/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.util;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Computable;
import com.intellij.util.Alarm;
import org.jetbrains.annotations.NotNull;

public class InfinitePeriodicalTask {
    private final Alarm myUpdateAlarm;
    private final long delay;
    private final Computable<? extends LongRunningReadTask> taskProvider;
    private LongRunningReadTask currentTask;

    public InfinitePeriodicalTask(
            long delay, @NotNull Alarm.ThreadToUse threadToUse, Disposable parentDisposable,
            Computable<? extends LongRunningReadTask> taskProvider
    ) {
        myUpdateAlarm = new Alarm(threadToUse, parentDisposable);
        this.delay = delay;
        this.taskProvider = taskProvider;
    }

    public InfinitePeriodicalTask start() {
        myUpdateAlarm.addRequest(new Runnable() {
            @Override
            public void run() {
                myUpdateAlarm.addRequest(this, delay);
                LongRunningReadTask task = taskProvider.compute();
                task.init();

                //noinspection unchecked
                if (task.shouldStart(currentTask)) {
                    currentTask = task;
                    currentTask.run();
                }
            }
        }, delay);

        return this;
    }
}
