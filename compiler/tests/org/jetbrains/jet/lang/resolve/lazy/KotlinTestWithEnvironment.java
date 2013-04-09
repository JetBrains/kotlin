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

package org.jetbrains.jet.lang.resolve.lazy;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.KotlinTestWithEnvironmentManagement;
import org.jetbrains.jet.TestCoreEnvironment;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.SubModuleDescriptor;
import org.jetbrains.jet.lang.resolve.KotlinModuleManager;
import org.jetbrains.jet.lang.resolve.ModuleSourcesManager;

public abstract class KotlinTestWithEnvironment extends KotlinTestWithEnvironmentManagement {
    private TestCoreEnvironment environment;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        environment = createEnvironment();
    }

    @Override
    protected void tearDown() throws Exception {
        environment = null;
        super.tearDown();
    }

    protected abstract TestCoreEnvironment createEnvironment();

    @NotNull
    public TestCoreEnvironment getEnvironment() {
        return environment;
    }

    @NotNull
    public Project getProject() {
        return getEnvironment().getProject();
    }

    @NotNull
    public ModuleSourcesManager getModuleSourcesManager() {
        return KotlinModuleManager.SERVICE.getService(getProject()).getSourcesManager();
    }

    @NotNull
    public ModuleDescriptor getModuleDescriptor() {
        return KotlinModuleManager.SERVICE.getService(getProject()).getModules().iterator().next();
    }

    @NotNull
    public SubModuleDescriptor getSubModuleDescriptor() {
        return getModuleDescriptor().getSubModules().iterator().next();
    }
}
