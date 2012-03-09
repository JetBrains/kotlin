/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve;

import com.google.inject.AbstractModule;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.types.lang.JetStandardLibrary;

/**
 * @author Stepan Koltsov
 */
public abstract class TopDownAnalysisModule extends AbstractModule {

    @NotNull
    private final Project project;
    private final boolean analyzingStdlib;

    protected TopDownAnalysisModule(@NotNull Project project, boolean analyzingStdlib) {
        this.project = project;
        this.analyzingStdlib = analyzingStdlib;
    }

    @Override
    protected final void configure() {
        binder().disableCircularProxies();

        bind(Project.class).toInstance(project);

        if (!analyzingStdlib) {
            // TODO: move outside
            JetStandardLibrary.initialize(project);
            bind(JetStandardLibrary.class).toInstance(JetStandardLibrary.getInstance());
        }

        configureAfter();
    }

    protected abstract void configureAfter();
}
