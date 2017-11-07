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
import org.gradle.api.Project
import org.gradle.process.ExecResult
import org.gradle.process.ExecSpec
import org.gradle.util.ConfigureUtil

import java.nio.file.Paths
import java.util.function.Function

/**
 * This class provides process.execRemote -- a drop-in replacement for process.exec.
 * If we provide -Premote=user@host the binaries are executed on a remote host
 * If we omit -Premote then the binary is executed locally as usual.
 */
class ExecRemote {

    private final Project project
    private final Function<Action<? super ExecSpec>, ExecResult> executor

    ExecRemote(Project project) {
        this.project = project
        if (project.hasProperty('remote')) {
            def remote = project.property('remote') as String
            executor = new SSHExecutor(remote)
        } else {
            executor = { project.exec(it) }
        }
    }

    ExecResult execRemote(Closure<? super ExecSpec> closure) {
        this.execRemote(ConfigureUtil.configureUsing(closure))
    }

    ExecResult execRemote(Action<? super ExecSpec> action) {
        this.executor.apply(action)
    }

    private class SSHExecutor implements Function<Action<? super ExecSpec>, ExecResult> {
        private final String remote
        private final String remoteDir

        SSHExecutor(String remote) {
            this.remote = remote
            this.remoteDir = uniqueSessionName()
        }

        @Override
        ExecResult apply(Action<? super ExecSpec> action) {
            String execFile

            createRemoteDir()
            def execResult = project.exec { ExecSpec execSpec ->
                action.execute(execSpec)
                execSpec.with {
                    upload(executable)
                    execFile = executable = "$remoteDir/${new File(executable).name}"
                    commandLine = ['/usr/bin/ssh', remote] + commandLine
                }
            }
            cleanup(execFile)
            return execResult
        }

        private String uniqueSessionName() {
            def date = new Date().format('yyyyMMddHHmmss')
            Paths.get(project.ext.remoteRoot.toString(), "tmp",
                    System.properties['user.name'].toString() + "_" + date).toString()
        }

        private createRemoteDir() {
            project.exec {
                commandLine('ssh', remote, 'mkdir', '-p', remoteDir)
            }
        }

        private upload(String fileName) {
            project.exec {
                commandLine('scp', fileName, "${remote}:${remoteDir}")
            }
        }

        private cleanup(String fileName) {
            project.exec {
                commandLine('ssh', remote, 'rm', fileName)
            }
        }
    }
}
