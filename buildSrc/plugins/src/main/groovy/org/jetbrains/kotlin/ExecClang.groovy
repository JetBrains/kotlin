/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin

import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.process.ExecResult
import org.gradle.process.ExecSpec
import org.gradle.util.ConfigureUtil

class ExecClang {

    private final Project project

    ExecClang(Project project) {
        this.project = project
    }

    public ExecResult execClang(Closure closure) {
        return this.execClang(ConfigureUtil.configureUsing(closure));
    }

    public ExecResult execClang(Action<? super ExecSpec> action) {
        Action<? super ExecSpec> extendedAction = new Action<ExecSpec>() {
            @Override
            void execute(ExecSpec execSpec) {
                action.execute(execSpec)

                execSpec.with {

                    if (executable == null) {
                        executable = 'clang'
                    }

                    if (executable in ['clang', 'clang++']) {
                        executable = "${project.llvmDir}/bin/$executable"
                    } else {
                        throw new GradleException("unsupported clang executable: $executable")
                    }

                    environment["PATH"] = project.files(project.clangPath).asPath +
                            File.pathSeparator + environment["PATH"]

                    args project.clangArgs
                }

            }
        }
        return project.exec(extendedAction)
    }
}
