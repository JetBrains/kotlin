package org.jetbrains.kotlin

import groovy.io.FileType
import org.gradle.api.DefaultTask
import org.gradle.api.internal.tasks.testing.detection.DefaultTestExecuter
import org.gradle.api.tasks.ParallelizableTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.testing.Test

abstract class KonanTest extends DefaultTask {
    protected String source
    def backendNative = project.project(":backend.native")
    def runtimeProject = project.project(":runtime")
    def dist = project.rootProject.file("dist")
    def llvmLlc = llvmTool("llc")
    def runtimeBc = new File("${dist.canonicalPath}/lib/runtime.bc").absolutePath
    def launcherBc = new File("${dist.canonicalPath}/lib/launcher.bc").absolutePath
    def startKtBc = new File("${dist.canonicalPath}/lib/start.kt.bc").absolutePath
    def stdlibKtBc = new File("${dist.canonicalPath}/lib/stdlib.kt.bc").absolutePath
    def mainC = 'main.c'
    String goldValue = null
    String testData = null
    List<String> arguments = null

    boolean enabled = true

    public void setDisabled(boolean value) {
        this.enabled = !value
    }

    public KonanTest(){
        // TODO: that's a long reach up the project tree.
        // May be we should reorganize a little.
        //dependsOn(project.parent.parent.tasks['dist'])
        dependsOn(project.rootProject.tasks['dist'])
    }

    abstract void compileTest(List<String> filesToCompile, String exe)

    protected void runCompiler(List<String> filesToCompile, String output, List<String> moreArgs) {
        project.javaexec {
            main = 'org.jetbrains.kotlin.cli.bc.K2NativeKt'
            classpath = project.configurations.cli_bc
            jvmArgs "-ea",
                    "-Dkonan.home=${dist.canonicalPath}",
                    "-Djava.library.path=${dist.canonicalPath}/konan/nativelib"
            args("-output", output,
                    *filesToCompile,
                    *moreArgs,
                    *project.globalArgs)
        }
    }

    protected void runCompiler(String source, String output, List<String> moreArgs) {
        runCompiler([source], output, moreArgs)
    }

    String buildExePath() {
        def exeName = project.file(source).name.replace(".kt", ".kt.exe")
        def tempDir = temporaryDir.absolutePath
        return "$tempDir/$exeName"
    }

    List<String> buildCompileList() {
        return [project.file(source).absolutePath]
    }

    @TaskAction
    void executeTest() {
        def exe = buildExePath()

        compileTest(buildCompileList(), exe)
        println "execution :$exe"

        def out = null
        project.exec {
            commandLine exe
            if (arguments != null) {
                args arguments
            }
            if (testData != null) {
                standardInput = new ByteArrayInputStream(testData.bytes)
            }
            if (goldValue != null) {
                out = new ByteArrayOutputStream()
                standardOutput = out
            }
        }
        if (goldValue != null && goldValue != out.toString())
            throw new RuntimeException("test failed.")
    }

    private String llvmTool(String tool) {
        return "${project.llvmDir}/bin/${tool}"
    }

    protected List<String> clangLinkArgs() {
        return project.clangLinkArgs
    }
}

class RunKonanTest extends KonanTest {
    void compileTest(List<String> filesToCompile, String exe) {
        runCompiler(filesToCompile, exe, [])
    }
}

class LinkKonanTest extends KonanTest {
    protected String lib

    void compileTest(List<String> filesToCompile, String exe) {
        def libDir = project.file(lib).absolutePath
        def libBc = "${libDir}.bc"

        runCompiler(lib, libBc, ['-nolink', '-nostdlib'])
        runCompiler(filesToCompile, exe, ['-library', libBc])
    }
}

class RunExternalTest extends RunKonanTest {

    String goldValue = "OK"

    String buildExePath() {
        def exeName = "${name}.kt.exe"
        def tempDir = temporaryDir.absolutePath
        return "$tempDir/$exeName"
    }

    // TODO refactor
    List<String> buildCompileList() {
        def result = []
        def filePattern = ~/(?m)\/\/\s*FILE:\s*(.*)$/
        def packagePattern = ~/(?m)package\s*([a-zA-z-][a-zA-Z0-9.-]*)/  //TODO check the regex
        def boxPattern = ~/(?m)fun\s*box\s*\(\s*\)/
        def boxPackage = ""
        def srcFile = project.file(source)
        def srcText = srcFile.text
        def matcher = filePattern.matcher(srcText)
        def tmpDir = temporaryDir.absolutePath

        if (!matcher.find()) {
            // There is only one file in the input
            project.copy{
                from srcFile.absolutePath
                into tmpDir
            }
            def newFile ="$tmpDir/${srcFile.name}"
            if (srcText =~ boxPattern && srcText =~ packagePattern){
                boxPackage = (srcText =~ packagePattern)[0][1]
                boxPackage += '.'
            }
            result.add(newFile)
        } else {
            // There are several files
            def processedChars = 0
            while (true) {
                def filePath = matcher.group(1)
                filePath = "$tmpDir/$filePath"
                def start = processedChars
                def nextFileExists = matcher.find()
                def end = nextFileExists ? matcher.start() : srcText.length()
                def fileText = srcText.substring(start, end)
                processedChars = end
                createFile(filePath, fileText)
                if (fileText =~ boxPattern && fileText =~ packagePattern){
                    boxPackage = (fileText =~ packagePattern)[0][1]
                    boxPackage += '.'
                }
                result.add(filePath)
                if (!nextFileExists) break
            }
        }
        createLauncherFile("$tmpDir/_launcher.kt", boxPackage)
        result.add("$tmpDir/_launcher.kt")
        return result
    }

    void createLauncherFile(String file, String pkg) {
        createFile(file, "fun main(args : Array<String>) { print(${pkg}box()) }")
    }

    void createFile(String file, String text) {
        project.file(file).write(text)
    }
}

class RunExternalTestGroup extends RunExternalTest {
    def groupDirectory = "."
    def logFileName = "test-result.md"
    String filter = project.property("filter")

    @TaskAction
    void executeTest() {
        def logFile = project.file(logFileName)
        logFile.write("|Test|Status|Comment|\n|----|------|-------|")
        def ktFiles = project.file(groupDirectory).listFiles(new FileFilter() {
            @Override
            boolean accept(File pathname) {
                pathname.isFile() && pathname.name.endsWith(".kt")
            }
        })
        if (filter != null) {
            def pattern = ~filter
            ktFiles = ktFiles.findAll {
                it.name =~ pattern
            }
        }
        def current = 0
        def passed = 0
        def total = ktFiles.size()
        ktFiles.each {
            source = project.relativePath(it)
            println("TEST: ${++current}/$total (passed: $passed)")
            try {
                super.executeTest()
                logFile.append("\n|$it.name|PASSED||")
                println("TEST PASSED\n")
                passed++
            } catch (Exception ex) {
                println("TEST FAILED\n")
                logFile.append("\n|$it.name|FAILED|${ex.getMessage()}. Cause: ${ex.getCause()?.getMessage()}|")
            }
        }
    }
}

