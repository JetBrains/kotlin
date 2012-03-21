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

package org.jetbrains.jet.plugin.run;

import org.jetbrains.annotations.Nullable;

/**
 * Should be serializable.
 *
 * @author Nikolay Krasko
 */
public class JetRunConfigurationSettings {
    public static String DEFAULT_MAIN_CLASS_NAME = "";
    public static String DEFAULT_VM_PARAMETERS = "";
    public static String DEFAULT_PROGRAM_PARAMETERS = "";
    public static String DEFAULT_WORKING_DIRECTORY = "";

    @Nullable
    private String mainClassName = DEFAULT_MAIN_CLASS_NAME;

    @Nullable
    private String vmParameters = DEFAULT_VM_PARAMETERS;

    @Nullable
    private String programParameters = DEFAULT_PROGRAM_PARAMETERS;

    @Nullable
    private String workingDirectory = DEFAULT_WORKING_DIRECTORY;

    @Nullable
    public String getMainClassName() {
        return mainClassName;
    }

    public void setMainClassName(@Nullable String mainClassName) {
        this.mainClassName = mainClassName;
    }

    @Nullable
    public String getVmParameters() {
        return vmParameters;
    }

    public void setVmParameters(@Nullable String vmParameters) {
        this.vmParameters = vmParameters;
    }

    @Nullable
    public String getProgramParameters() {
        return programParameters;
    }

    public void setProgramParameters(@Nullable String programParameters) {
        this.programParameters = programParameters;
    }

    @Nullable
    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public void setWorkingDirectory(@Nullable String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }
}
