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

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget

class CompileCppToBitcode extends DefaultTask {
    private String name = "main"
    private String target = "host"
    private File srcRoot;

    protected List<String> compilerArgs = []
    protected List<String> linkerArgs = []

    @InputDirectory
    File getSrcRoot() {
        return srcRoot ?: project.file("src/$name")
    }

    @OutputFile
    File getOutFile() {
        return new File(getTargetDir(), "${name}.bc")
    }

    private File getSrcDir() {
        return new File(this.getSrcRoot(), "cpp")
    }

    private File getHeadersDir() {
        return new File(this.getSrcRoot(), "headers")
    }

    private File getTargetDir() {
        return new File(project.buildDir, target)
    }

    private File getObjDir() {
        return new File(getTargetDir(), name)
    }

    void name(String value) {
        name = value
    }

    void target(String value) {
        target = value
    }

    void srcRoot(File value) {
        srcRoot = value
    }

    protected List<String> getCompilerArgs() {
        return compilerArgs
    }

    protected List<String> getLinkerArgs() {
        return linkerArgs
    }

    protected  String getTarget() {
        return target
    }

    void compilerArgs(String... args) {
        compilerArgs.addAll(args)
    }

    void compilerArgs(List<String> args) {
        compilerArgs.addAll(args)
    }

    void linkerArgs(String... args) {
        linkerArgs.addAll(args)
    }

    void linkerArgs(List<String> args) {
        linkerArgs.addAll(args)
    }

    private Boolean targetingMinGW() {
        def hostManager = new HostManager()
        return hostManager.targetByName(this.target).family == Family.MINGW
    }

    @TaskAction
    void compile() {
        // the strange code below seems to be required due to some Gradle (Groovy?) behaviour
        File headersDir = this.getHeadersDir()
        File srcDir = this.getSrcDir()
        List<String> compilerArgs = this.getCompilerArgs()
        List<String> linkerArgs = this.getLinkerArgs()
        File objDir = this.getObjDir()
        objDir.mkdirs()
        Boolean targetingMinGW = this.targetingMinGW()

        project.execKonanClang(this.target) {
            workingDir objDir
            executable "clang++"
            args '-std=c++14'
            args '-Werror'
            args '-O2'
            if (!targetingMinGW) {
                args '-fPIC'
            }
            args compilerArgs

            args "-I$headersDir"
            args '-c', '-emit-llvm'
            args project.fileTree(srcDir) {
                include('**/*.cpp')
                include('**/*.mm') // Objective-C++
            }
        }

        project.exec {
            executable "$project.llvmDir/bin/llvm-link"
            args project.fileTree(objDir).include('**/*.bc').sort { a, b -> (a.name <=> b.name) }

            args linkerArgs

            args '-o', outFile
        }
    }
}
