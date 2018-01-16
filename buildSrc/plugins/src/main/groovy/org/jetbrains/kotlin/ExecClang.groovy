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
import org.jetbrains.kotlin.konan.target.*

class ExecClang {

    private final Project project

    ExecClang(Project project) {
        this.project = project
    }

    private def platformManager = project.rootProject.platformManager

    private List<String> konanArgs(KonanTarget target) {
        return platformManager.platform(target).clang.clangArgsForKonanSources
    }

    private List<String> konanArgs(String targetName) {
        def target = platformManager.targetManager(targetName).target
        return konanArgs(target)
    }

    // The bare ones invoke clang with system default sysroot.

    public ExecResult execBareClang(Action<? super ExecSpec> action) {
        return this.execClang([], action)
    }

    public ExecResult execBareClang(Closure closure) {
        return this.execClang([], closure)
    }

    // The konan ones invoke clang with konan provided sysroots.
    // So they require a target or assume it to be the host.
    // The target can be specified as KonanTarget or as a
    // (nullable, which means host) target name.

    public ExecResult execKonanClang(String target, Action<? super ExecSpec> action) {
        return this.execClang(konanArgs(target), action)
    }

    public ExecResult execKonanClang(KonanTarget target, Action<? super ExecSpec> action) {
        return this.execClang(konanArgs(target), action)
    }

    public ExecResult execKonanClang(String target, Closure closure) {
        return this.execClang(konanArgs(target), closure)
    }

    public ExecResult execKonanClang(KonanTarget target, Closure closure) {
        return this.execClang(konanArgs(target), closure)
    }

    // These ones are private, so one has to choose either Bare or Konan.

    private ExecResult execClang(List<String> defaultArgs, Closure closure) {
        return this.execClang(defaultArgs, ConfigureUtil.configureUsing(closure))
    }

    private ExecResult execClang(List<String> defaultArgs, Action<? super ExecSpec> action) {
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

                    environment["PATH"] = project.files(project.hostPlatform.clang.clangPaths).asPath +
                            File.pathSeparator + environment["PATH"]
                    args defaultArgs
                }
            }
        }
        return project.exec(extendedAction)
    }
}
