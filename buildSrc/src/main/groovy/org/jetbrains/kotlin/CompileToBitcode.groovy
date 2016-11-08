package org.jetbrains.kotlin

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

class CompileCppToBitcode extends DefaultTask {
    private String name = "main"
    private File srcRoot;

    private List<String> compilerArgs = []
    private List<String> linkerArgs = []

    @InputDirectory
    File getSrcRoot() {
        return srcRoot ?: project.file("src/$name")
    }

    @OutputFile
    File getOutFile() {
        return new File(project.buildDir, "${name}.bc")
    }

    private File getSrcDir() {
        return new File(this.getSrcRoot(), "cpp")
    }

    private File getHeadersDir() {
        return new File(this.getSrcRoot(), "headers")
    }

    private File getObjDir() {
        return new File(project.buildDir, name)
    }

    void name(String value) {
        name = value
    }

    void srcRoot(File value) {
        srcRoot = value
    }

    private List<String> getCompilerArgs() {
        return compilerArgs
    }

    private List<String> getLinkerArgs() {
        return linkerArgs
    }

    void compilerArgs(String... args) {
        compilerArgs.addAll(args)
    }

    void linkerArgs(String... args) {
        linkerArgs.addAll(args)
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

        project.execClang {
            workingDir objDir
            executable "clang++"
            args '-std=c++11'

            args compilerArgs

            args "-I$headersDir"

            args '-c', '-emit-llvm'
            args project.fileTree(srcDir).include('**/*.cpp')
        }

        project.exec {
            executable "$project.llvmDir/bin/llvm-link"
            args project.fileTree(objDir).include('**/*.bc')

            args linkerArgs

            args '-o', outFile
        }
    }
}
