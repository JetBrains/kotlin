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

package org.jetbrains.jet.plugin;

import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

/**
 * This project component initializes KotlinBuiltIns so that throwing a ProcessCanceledException while
 * loading PSI from declaration files is prevented.
 */
public class BuiltInsInitializer {
    public BuiltInsInitializer(@NotNull final Project project) {
        ProgressManager.getInstance().executeNonCancelableSection(new Runnable() {
            @Override
            public void run() {
                // MULTI_THREADED is important not only because of threading as such: since built-ins rely on a project,
                // if they are not fully initialized when a project is closed,
                // they will be referencing invalid PSI upon a next request
                KotlinBuiltIns.initialize(project, KotlinBuiltIns.InitializationMode.MULTI_THREADED);
            }
        });
    }
}
