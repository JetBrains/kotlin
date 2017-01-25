package org.jetbrains.kotlin

import groovy.json.JsonOutput
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

    // TODO refactor
    List<String> buildCompileList() {
        def result = []
        def filePattern = ~/(?m)\/\/\s*FILE:\s*(.*)$/
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
            result.add(newFile)
        } else {
            // There are several files
            def processedChars = 0
            while (true) {
                def filePath = "$outputDirectory/${matcher.group(1)}"
                def start = processedChars
                def nextFileExists = matcher.find()
                def end = nextFileExists ? matcher.start() : srcText.length()
                def fileText = srcText.substring(start, end)
                processedChars = end
                createFile(filePath, fileText)
                result.add(filePath)
                if (!nextFileExists) break
            }
        }
        return result
    }

    void createFile(String file, String text) {
        project.file(file).write(text)
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

@ParallelizableTask
class RunKonanTest extends KonanTest {
    void compileTest(List<String> filesToCompile, String exe) {
        runCompiler(filesToCompile, exe, [])
    }
}

@ParallelizableTask
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
    Map<String, TestResult> results = [:]
    Statistics statistics = new Statistics()

    static class TestResult {
        String status = null
        String comment = null

        TestResult(String status, String comment = ""){
            this.status = status;
            this.comment = comment;
        }
    }

    static class Statistics {
        int total = 0
        int passed = 0
        int failed = 0
        int skipped = 0

        void pass(int count = 1) {
            passed += count
            total += count
        }

        void skip(int count = 1) {
            skipped += count
            total += count
        }

        void fail(int count = 1) {
            failed += count
            total += count
        }

        void add(Statistics other) {
            total   += other.total
            passed  += other.passed
            failed  += other.failed
            skipped += other.skipped
        }
    }

    List<String> buildCompileList() {
        def packagePattern = ~/(?m)package\s*([a-zA-z-][a-zA-Z0-9.-]*)/  //TODO check the regex
        def boxPattern = ~/(?m)fun\s*box\s*\(\s*\)/
        def boxPackage = ""

        def result = super.buildCompileList()
        for (String filePath : result) {
            def text = project.file(filePath).text
            if (text =~ boxPattern && text =~ packagePattern){
                boxPackage = (text =~ packagePattern)[0][1]
                boxPackage += '.'
                break
            }
        }
        createLauncherFile("$outputDirectory/_launcher.kt", boxPackage)
        result.add("$outputDirectory/_launcher.kt")
        return result
    }

    void createLauncherFile(String file, String pkg) {
        createFile(file, "fun main(args : Array<String>) { print(${pkg}box()) }")
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
        List<File> ktFiles = project.file(groupDirectory).listFiles(new FileFilter() {
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
        def currentResult = null
        statistics = new Statistics()
        ktFiles.each {
            source = project.relativePath(it)
            println("TEST: $it.name ($statistics.total/${ktFiles.size()}, passed: $statistics.passed, skipped: $statistics.skipped)")
            if (isEnabledForNativeBackend(source)) {
                try {
                    super.executeTest()
                    currentResult = new TestResult("PASSED")
                    statistics.pass()
                } catch (Exception ex) {
                    currentResult = new TestResult("FAILED",
                            "Exception: ${ex.getMessage()}. Cause: ${ex.getCause()?.getMessage()}")
                    statistics.fail()
                }
            } else {
                currentResult = new TestResult("SKIPPED")
                statistics.skip()
            }
            println("TEST $currentResult.status\n")
            results.put(it.name, currentResult)
        }

        // Save the report.
        def reportFile = project.file("${outputDirectory}/results.json")
        def json = JsonOutput.toJson(["statistics" : statistics, "tests" : results])
        reportFile.write(JsonOutput.prettyPrint(json))
        println("TOTAL PASSED: $statistics.passed/$statistics.total (SKIPPED: $statistics.skipped)")
    }
}
