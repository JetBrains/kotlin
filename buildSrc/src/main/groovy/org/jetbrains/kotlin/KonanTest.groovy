package org.jetbrains.kotlin

import groovy.json.JsonBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.ParallelizableTask
import org.gradle.api.tasks.TaskAction

abstract class KonanTest extends DefaultTask {
    protected String source
    def backendNative = project.project(":backend.native")
    def runtimeProject = project.project(":runtime")
    def dist = project.rootProject.file("dist")
    def runtimeBc = new File("${dist.canonicalPath}/lib/runtime.bc").absolutePath
    def launcherBc = new File("${dist.canonicalPath}/lib/launcher.bc").absolutePath
    def startKtBc = new File("${dist.canonicalPath}/lib/start.kt.bc").absolutePath
    def stdlibKtBc = new File("${dist.canonicalPath}/lib/stdlib.kt.bc").absolutePath
    def mainC = 'main.c'
    def outputSourceSetName = "testOutputLocal"
    String outputDirectory = null
    String goldValue = null
    String testData = null
    List<String> arguments = null

    boolean enabled = true

    public void setDisabled(boolean value) {
        this.enabled = !value
    }

    // Uses directory defined in $outputSourceSetName source set.
    // If such source set doesn't exist, uses temporary directory.
    public void createOutputDirectory() {
        if (outputDirectory != null) {
            return
        }

        def outputSourceSet = project.sourceSets.findByName(getOutputSourceSetName())
        if (outputSourceSet != null) {
            outputDirectory = outputSourceSet.output.getDirs().getSingleFile().absolutePath + "/$name"
            project.file(outputDirectory).mkdirs()
        } else {
            outputDirectory = getTemporaryDir().absolutePath
        }
    }

    public KonanTest(){
        // TODO: that's a long reach up the project tree.
        // May be we should reorganize a little.
        dependsOn(project.rootProject.tasks['dist'])
    }

    abstract void compileTest(List<String> filesToCompile, String exe)

    protected void runCompiler(List<String> filesToCompile, String output, List<String> moreArgs) {
        def log = new ByteArrayOutputStream()
        try {
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
                standardOutput = log
                errorOutput = log
            }
        } finally {
            def logString = log.toString()
            project.file("${output}.compilation.log").write(logString)
            println(logString)
        }
    }

    protected void runCompiler(String source, String output, List<String> moreArgs) {
        runCompiler([source], output, moreArgs)
    }

    String buildExePath() {
        def exeName = project.file(source).name.replace(".kt", ".kt.exe")
        return "$outputDirectory/$exeName"
    }

    List<String> buildCompileList() {
        return [project.file(source).absolutePath]
    }

    @TaskAction
    void executeTest() {
        createOutputDirectory()
        def exe = buildExePath()

        compileTest(buildCompileList(), exe)
        println "execution :$exe"

        def out = null
        //TODO Add test timeout
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

@ParallelizableTask
class RunExternalTestGroup extends RunKonanTest {

    def groupDirectory = "."
    def outputSourceSetName = "testOutputExternal"
    String filter = project.findProperty("filter")
    String goldValue = "OK"

    class TestResult {
        String name = null
        String status = null
        String comment = null

        TestResult(String name, String status, String comment){
            this.name = name;
            this.status = status;
            this.comment = comment;
        }
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

        if (!matcher.find()) {
            // There is only one file in the input
            project.copy{
                from srcFile.absolutePath
                into outputDirectory
            }
            def newFile ="$outputDirectory/${srcFile.name}"
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
                filePath = "$outputDirectory/$filePath"
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
        createLauncherFile("$outputDirectory/_launcher.kt", boxPackage)
        result.add("$outputDirectory/_launcher.kt")
        return result
    }

    void createLauncherFile(String file, String pkg) {
        createFile(file, "fun main(args : Array<String>) { print(${pkg}box()) }")
    }

    void createFile(String file, String text) {
        project.file(file).write(text)
    }

    List<String> findLinesWithPrefixesRemoved(String text, String prefix) {
        def result = []
        text.eachLine {
            if (it.startsWith(prefix)) {
                result.add(it - prefix)
            }
        }
        return result
    }

    boolean isEnabledForNativeBackend(String fileName) {
        def text = project.file(fileName).text
        def targetBackend = findLinesWithPrefixesRemoved(text, "// TARGET_BACKEND")
        if (targetBackend.size() != 0) {
            // There is some target backend. Check if it is NATIVE or not.
            for (String s : targetBackend) {
                if (s.contains("NATIVE")){ return true }
            }
            return false
        } else {
            // No target backend. Check if NATIVE backend is ignored.
            def ignoredBackends = findLinesWithPrefixesRemoved(text, "// IGNORE_BACKEND: ")
            for (String s : ignoredBackends) {
                if (s.contains("NATIVE")) { return false }
            }
            return true
        }
    }

    @TaskAction
    @Override
    void executeTest() {
        createOutputDirectory()

        // Form the test list.
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

        // Run the tests.
        def current = 0
        def passed = 0
        def skipped = 0
        def failed = 0
        def total = ktFiles.size()
        def results = []
        def currentResult = null
        ktFiles.each {
            current++
            source = project.relativePath(it)
            println("TEST: $it.name ($current/$total, passed: $passed, skipped: $skipped)")
            if (isEnabledForNativeBackend(source)) {
                try {
                    super.executeTest()
                    currentResult = new TestResult(it.name, "PASSED", "")
                    passed++
                } catch (Exception ex) {
                    currentResult = new TestResult(it.name, "FAILED",
                            "Exception: ${ex.getMessage()}. Cause: ${ex.getCause()?.getMessage()}")
                    failed++
                }
            } else {
                currentResult = new TestResult(it.name, "SKIPPED", "")
                skipped++
            }
            println("TEST $currentResult.status\n")
            results.add(currentResult)
        }

        // Save the report.
        def reportFile = project.file("${outputDirectory}/results.json")
        def json = new JsonBuilder()
        json {
            "statistics" "total" : total, "passed" : passed, "failed" : failed, "skipped" : skipped

            "tests" {
                results.each { result ->
                    "$result.name" {
                        "status" result.status
                        "comment" result.comment
                    }
                }
            }


        }
        reportFile.write(json.toPrettyString())
        println("TOTAL PASSED: $passed/$total (SKIPPED: $skipped)")
    }
}
