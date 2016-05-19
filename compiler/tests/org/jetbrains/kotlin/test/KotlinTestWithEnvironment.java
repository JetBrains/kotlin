/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.utils.ExceptionUtilsKt;

import java.lang.reflect.Field;

public abstract class KotlinTestWithEnvironment extends KotlinTestWithEnvironmentManagement {
    private KotlinCoreEnvironment environment;
    private Application application;

    @Override
    protected void setUp() throws Exception {
        application = ApplicationManager.getApplication();

        super.setUp();
        environment = createEnvironment();
    }

    @Override
    protected void tearDown() throws Exception {
        removeEnvironment();
        environment = null;
        super.tearDown();

        if (application == null) {
            resetApplicationToNull();
        }

        application = null;
    }

    protected void resetApplicationToNull() {
        try {
            Field ourApplicationField = ApplicationManager.class.getDeclaredField("ourApplication");
            ourApplicationField.setAccessible(true);
            ourApplicationField.set(null, null);
        }
        catch (Exception e) {
            throw ExceptionUtilsKt.rethrow(e);
        }
    }

    protected abstract KotlinCoreEnvironment createEnvironment() throws Exception;
    protected void removeEnvironment() throws Exception {}

    @NotNull
    public KotlinCoreEnvironment getEnvironment() {
        return environment;
    }

    @NotNull
    public Project getProject() {
        return getEnvironment().getProject();
    }
}
