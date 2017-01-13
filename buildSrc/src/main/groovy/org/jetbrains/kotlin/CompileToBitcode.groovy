package org.jetbrains.kotlin

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

class CompilePerTarget extends CompileCppToBitcode {
    private List<String> targetList = []
    private Map<String, List<String>> targetArgs = null
    private Map<String, List<String>> targetLinkerArgs = null

    void targetList(List<String> list) {
        targetList.addAll(list)
    }

    void targetArgs(Map<String, List<String>> map) {
        targetArgs = map
    }

    private Map<String, List<String>> getTargetArgs() {
        return targetArgs
    }

    void targetLinkerArgs(Map<String, List<String>> map) {
        targetLinkerArgs = map
    }

    private Map<String, List<String>> getTargetLinkerArgs() {
        return targetLinkerArgs
    }

    @TaskAction
    void compile() {
        def targetList = this.targetList
        def targetArgs = getTargetArgs()
        def targetLinkerArgs = getTargetLinkerArgs()
        def commonCompilerArgs = getCompilerArgs().clone()
        def commonLinkerArgs = getLinkerArgs().clone()
        targetList.each {
            this.compilerArgs = [] 
            this.linkerArgs = []
            target(it)
            compilerArgs(commonCompilerArgs)
            if (targetArgs != null)
                compilerArgs(targetArgs[it])
            linkerArgs(commonLinkerArgs)
            if (targetLinkerArgs != null)
                linkerArgs(targetLinkerArgs[it])
            super.compile()
        }
    }
}

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
