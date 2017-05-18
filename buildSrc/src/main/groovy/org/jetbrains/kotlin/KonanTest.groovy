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

import groovy.json.JsonOutput
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.ParallelizableTask
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecResult

abstract class KonanTest extends JavaExec {
    protected String source
    def backendNative = project.project(":backend.native")
    def runtimeProject = project.project(":runtime")
    def dist = project.rootProject.file("dist")
    def dependenciesDir = project.findProject(":dependencies").file("all")
    def runtimeBc = new File("${dist.canonicalPath}/lib/runtime.bc").absolutePath
    def launcherBc = new File("${dist.canonicalPath}/lib/launcher.bc").absolutePath
    def startKtBc = new File("${dist.canonicalPath}/lib/start.kt.bc").absolutePath
    def stdlibKtBc = new File("${dist.canonicalPath}/lib/stdlib.kt.bc").absolutePath
    def konanc = new File("${dist.canonicalPath}/bin/konanc").absolutePath
    def mainC = 'main.c'
    def outputSourceSetName = "testOutputLocal"
    String outputDirectory = null
    String goldValue = null
    String testData = null
    int expectedExitStatus = 0
    List<String> arguments = null
    List<String> flags = null

    boolean enabled = true
    boolean run = true

    void setDisabled(boolean value) {
        this.enabled = !value
    }

    // Uses directory defined in $outputSourceSetName source set.
    // If such source set doesn't exist, uses temporary directory.
    void createOutputDirectory() {
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

    KonanTest(){
        // TODO: that's a long reach up the project tree.
        // May be we should reorganize a little.
        dependsOn(project.rootProject.tasks['dist'])
    }

    @Override
    void setJvmArgs(Iterable<?> arguments) {
        super.setJvmArgs(arguments +
                "-Dkonan.home=${dist.canonicalPath}" +
                "-Djava.library.path=${dist.canonicalPath}/konan/nativelib")
    }

    @Override
    void exec() {
        // Perhaps later we will return this exec() back but for now rest of infrastructure expects
        // compilation begins on runCompiler call, to emulate this behaviour we call super.exec() after
        // configuration part at runCompiler.
    }

    abstract void compileTest(List<String> filesToCompile, String exe)

    protected void runCompiler(List<String> filesToCompile, String output, List<String> moreArgs) {
        def log = new ByteArrayOutputStream()
        try {
            main = 'org.jetbrains.kotlin.cli.bc.K2NativeKt'
            classpath = project.configurations.cli_bc
            enableAssertions = true
            args = ["-output", output,
                    "-ea",
                    *filesToCompile,
                    *moreArgs,
                    *project.globalTestArgs]
            if (project.testTarget) {
                args "-target", project.testTarget
            }
            standardOutput = log
            errorOutput = log
            super.exec()
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
        def exeName = project.file(source).name.replace(".kt", "")
        return "$outputDirectory/$exeName"
    }

    protected String removeDiagnostics(String str) {
        return str.replaceAll(~/<!.*?!>(.*?)<!>/) { all, text -> text }
    }

    protected List<String> registerKtFile(List<String> sourceFiles, String newFilePath, String newFileContent) {
        createFile(newFilePath, newFileContent)
        if (newFilePath.endsWith(".kt")) {
            sourceFiles.add(newFilePath)
        }
        return sourceFiles
    }

    void createCoroutineUtil(String file) {
        StringBuilder text = new StringBuilder("import kotlin.coroutines.experimental.*\n")
        text.append(
                """
open class EmptyContinuation(override val context: CoroutineContext = EmptyCoroutineContext) : Continuation<Any?> {
    companion object : EmptyContinuation()
    override fun resume(value: Any?) {}
    override fun resumeWithException(exception: Throwable) { throw exception }
}

fun <T> handleResultContinuation(x: (T) -> Unit): Continuation<T> = object: Continuation<T> {
    override val context = EmptyCoroutineContext
    override fun resumeWithException(exception: Throwable) {
        throw exception
    }

    override fun resume(data: T) = x(data)
}

fun handleExceptionContinuation(x: (Throwable) -> Unit): Continuation<Any?> = object: Continuation<Any?> {
    override val context = EmptyCoroutineContext
    override fun resumeWithException(exception: Throwable) {
        x(exception)
    }

    override fun resume(data: Any?) { }
}
"""     )
        createFile(file, text.toString())
    }

    // TODO refactor
    List<String> buildCompileList() {
        def result = []
        def filePattern = ~/(?m)\/\/\s*FILE:\s*(.*)$/
        def srcFile = project.file(source)
        def srcText = removeDiagnostics(srcFile.text)
        def matcher = filePattern.matcher(srcText)

        if (srcText.contains('// WITH_COROUTINES')) {
            def coroutineUtilFileName = "$outputDirectory/CoroutineUtil.kt"
            createCoroutineUtil(coroutineUtilFileName)
            result.add(coroutineUtilFileName)
        }

        if (!matcher.find()) {
            // There is only one file in the input
            def filePath = "$outputDirectory/${srcFile.name}"
            registerKtFile(result, filePath, srcText)
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
                registerKtFile(result, filePath, fileText)
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
        def program = buildExePath()
        def exe = "${program}.kexe"

        compileTest(buildCompileList(), program)

        if (!run) {
            println "to be executed manually: $exe"
            return
        }
        println "execution: $exe"

        def out = new ByteArrayOutputStream()
        //TODO Add test timeout
        ExecResult execResult = project.execRemote {
            commandLine exe
            if (arguments != null) {
                args arguments
            }
            if (testData != null) {
                standardInput = new ByteArrayInputStream(testData.bytes)
            }
            standardOutput = out

            ignoreExitValue = true
        }
        println(out.toString())

        if (execResult.exitValue != expectedExitStatus) {
            throw new TestFailedException(
                    "Test failed. Expected exit status: $expectedExitStatus, actual: ${execResult.exitValue}")
        }

        if (goldValue != null && goldValue != out.toString()) {
            throw new TestFailedException("Test failed. Expected output: $goldValue, actual output: ${out.toString()}")
        }
    }
}

class TestFailedException extends RuntimeException {
    TestFailedException(String s) {
        super(s)
    }
}
@ParallelizableTask
class RunKonanTest extends KonanTest {
    void compileTest(List<String> filesToCompile, String exe) {
        runCompiler(filesToCompile, exe, flags?:[])
    }
}

// This is another way to run the compiler.
// Don't use this task for regular testing as
// project.exec + a shell script isolate the jvm
// from IDEA. Use the RunKonanTest instead.
@ParallelizableTask
class RunDriverKonanTest extends KonanTest {

    RunDriverKonanTest() {
        super()
        dependsOn(project.rootProject.tasks['cross_dist'])
    }

    void compileTest(List<String> filesToCompile, String exe) {
        runCompiler(filesToCompile, exe, flags?:[])
    }

    protected void runCompiler(List<String> filesToCompile, String output, List<String> moreArgs) {
        def log = new ByteArrayOutputStream()
        project.exec {
            commandLine konanc
            args = ["-output", output,
                    "-ea",
                    *filesToCompile,
                    *moreArgs,
                    *project.globalTestArgs]
            if (project.testTarget) {
                args "-target", project.testTarget
            }
            standardOutput = log
            errorOutput = log
        }
        def logString = log.toString()
        project.file("${output}.compilation.log").write(logString)
        println(logString)
    }
}

@ParallelizableTask
class RunInteropKonanTest extends KonanTest {

    private String interop
    private NamedNativeInteropConfig interopConf

    void setInterop(String value) {
        this.interop = value
        this.interopConf = project.kotlinNativeInterop[value]
        this.interopConf.target = project.testTarget
        this.dependsOn(this.interopConf.genTask)
    }

    void compileTest(List<String> filesToCompile, String exe) {
        String interopBc = exe + "-interop.bc"
        runCompiler([interopConf.generatedSrcDir.absolutePath], interopBc, ["-nolink"])

        String interopStubsBc = new File(interopConf.nativeLibsDir, interop + "stubs.bc").absolutePath

        List<String> linkerArguments = interopConf.linkerOpts // TODO: add arguments from .def file

        List<String> compilerArguments = ["-library", interopBc, "-nativelibrary", interopStubsBc] +
                linkerArguments.collectMany { ["-linkerArgs", it] }

        runCompiler(filesToCompile, exe, compilerArguments)
    }
}

@ParallelizableTask
class LinkKonanTestNoStdlib extends KonanTest {
    protected String lib

    void compileTest(List<String> filesToCompile, String exe) {
        def libDir = project.file(lib).absolutePath
        def libBc = "${libDir}.bc"

        runCompiler(lib, libBc, ['-nolink', '-nostdlib'])
        runCompiler(filesToCompile, exe, ['-library', libBc])
    }
}

@ParallelizableTask
class LinkKonanTest extends KonanTest {
    protected String lib

    void compileTest(List<String> filesToCompile, String exe) {
        def libDir = project.file(lib).absolutePath
        def libBc = "${libDir}.bc"

        runCompiler(lib, libBc, ['-nolink'] + ((flags != null) ? flags :[]))
        runCompiler(filesToCompile, exe, ['-library', libBc] + ((flags != null) ? flags :[]))
    }
}

@ParallelizableTask
class RunExternalTestGroup extends RunKonanTest {

    def groupDirectory = "."
    def outputSourceSetName = "testOutputExternal"
    String filter = project.findProperty("filter")
    Map<String, TestResult> results = [:]
    Statistics statistics = new Statistics()

    RunExternalTestGroup() {
    }

    static enum TestStatus {
        PASSED,
        FAILED,
        ERROR,
        SKIPPED
    }
    static class TestResult {
        TestStatus status = null
        String comment = null

        TestResult(TestStatus status, String comment = ""){
            this.status = status;
            this.comment = comment;
        }
    }
    static class Statistics {
        int total = 0
        int passed = 0
        int failed = 0
        int error = 0
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

        void error(int count = 1) {
            error += count
            total += count
        }

        void add(Statistics other) {
            total   += other.total
            passed  += other.passed
            failed  += other.failed
            error   += other.error
            skipped += other.skipped
        }
    }

    List<String> buildCompileList() {
        def packagePattern = ~/(?m)^\s*package\s+([a-zA-z-][a-zA-Z0-9._-]*)/
        def boxPattern = ~/(?m)fun\s+box\s*\(\s*\)/
        def imports = []

        def result = super.buildCompileList()
        for (String filePath : result) {
            def text = project.file(filePath).text
            if (text =~ boxPattern && text =~ packagePattern){
                def pkg = (text =~ packagePattern)[0][1]
                imports.add("$pkg.*")
                break
            }
        }
        createLauncherFile("$outputDirectory/_launcher.kt", imports)
        result.add("$outputDirectory/_launcher.kt")
        result.add(project.file("testUtils.kt"))
        result.add(project.file("helpers.kt"))
        return result
    }

    /**
     * There are tests that require non-trivial 'package foo' in test launcher.
     */
    void createLauncherFile(String file, List<String> imports) {
        StringBuilder text = new StringBuilder("import kotlin.test.TestFailedException\n")
        for (v in imports) {
            text.append("import ").append(v).append('\n')
        }

        text.append(
"""
fun main(args : Array<String>) {
    @Suppress("UNUSED_VARIABLE")
    val result = box()
    ${ (goldValue != null) ? "print(result)" : "" }
}
"""     )
        createFile(file, text.toString())
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
        def outputRootDirectory = outputDirectory

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
        def testSuite = createTestSuite(name, statistics)
        testSuite.start()
        ktFiles.each {
            source = project.relativePath(it)
            // Create separate output directory for each test in the group.
            outputDirectory = outputRootDirectory + "/${it.name}"
            project.file(outputDirectory).mkdirs()
            println("TEST: $it.name (done: $statistics.total/${ktFiles.size()}, passed: $statistics.passed, skipped: $statistics.skipped)")
            def testCase = testSuite.createTestCase(it.name)
            testCase.start()
            if (isEnabledForNativeBackend(source)) {
                try {
                    super.executeTest()
                    currentResult = testCase.pass()
                } catch (TestFailedException e) {
                    currentResult = testCase.fail(e)
                } catch (Exception ex) {
                    currentResult = testCase.error(ex)
                }
            } else {
                currentResult = testCase.skip()
            }
            println("TEST $currentResult.status\n")
            if (currentResult.status == TestStatus.ERROR || currentResult.status == TestStatus.FAILED) {
                println("Command to reproduce: ./gradlew $name -Pfilter=${it.name}\n")
            }
            results.put(it.name, currentResult)
        }
        testSuite.finish()

        // Save the report.
        def reportFile = project.file("${outputRootDirectory}/results.json")
        def json = JsonOutput.toJson(["statistics" : statistics, "tests" : results])
        reportFile.write(JsonOutput.prettyPrint(json))
        println("TOTAL PASSED: $statistics.passed/$statistics.total (SKIPPED: $statistics.skipped)")
    }

    KonanTestSuite createTestSuite(String name, Statistics statistics) {
        if (System.getenv("TEAMCITY_BUILD_PROPERTIES_FILE") != null)
            return new TeamcityKonanTestSuite(name, statistics)
        return new KonanTestSuite(name, statistics)
    }

    class KonanTestSuite {
        protected name;
        protected statistics;

        KonanTestSuite(String name, Statistics statistics) {
            this.name       = name
            this.statistics = statistics
        }

        protected class KonanTestCase {
            protected name

            KonanTestCase(String name) {
                this.name = name
            }

            void start(){}

            TestResult pass() {
                statistics.pass()
                return new TestResult(TestStatus.PASSED)
            }

            TestResult fail(TestFailedException e) {
                statistics.fail()
                println(e.getMessage())
                return new TestResult(TestStatus.FAILED, "Exception: ${e.getMessage()}. Cause: ${e.getCause()?.getMessage()}")
            }

            TestResult error(Exception e) {
                statistics.error()
                return new TestResult(TestStatus.ERROR, "Exception: ${e.getMessage()}. Cause: ${e.getCause()?.getMessage()}")
            }

            TestResult skip() {
                statistics.skip()
                return new TestResult(TestStatus.SKIPPED)
            }
        }

        KonanTestCase createTestCase(String name) {
            return new KonanTestCase(name)
        }

        void start() {}
        void finish() {}
    }

    class TeamcityKonanTestSuite extends KonanTestSuite {
        TeamcityKonanTestSuite(String suiteName, Statistics statistics) {
            super(suiteName, statistics)
        }

        class TeamcityKonanTestCase extends KonanTestSuite.KonanTestCase {

            TeamcityKonanTestCase(String name) {
                super(name)
            }

            private teamcityFinish() {
                teamcityReport("testFinished name='$name'")
            }

            void start() {
                teamcityReport("testStarted name='$name'")
            }

            TestResult pass() {
                teamcityFinish()
                return super.pass()
            }

            TestResult fail(TestFailedException e) {
                teamcityReport("testFailed type='comparisonFailure' name='$name' message='${toTeamCityFormat(e.getMessage())}'")
                teamcityFinish()
                return super.fail(e)
            }

            TestResult error(Exception e) {
                def writer = new StringWriter()
                e.printStackTrace(new PrintWriter(writer))
                def rawString  = writer.toString()

                teamcityReport("testFailed name='$name' message='${toTeamCityFormat(e.getMessage())}' details='${toTeamCityFormat(rawString)}'")
                teamcityFinish()
                return super.error(e)
            }

            TestResult skip() {
                teamcityReport("testIgnored name='$name'")
                teamcityFinish()
                return super.skip()
            }
        }

        TeamcityKonanTestCase createTestCase(String name) {
            return new TeamcityKonanTestCase(name)
        }

        private teamcityReport(String msg) {
            println("##teamcity[$msg]")
        }

        /**
         * Teamcity require escaping some symbols in pipe manner.
         * https://github.com/GitTools/GitVersion/issues/94
         */
        String toTeamCityFormat(String inStr) {
            return inStr.replaceAll("\\|", "||")
                        .replaceAll("\r",  "|r")
                        .replaceAll("\n",  "|n")
                        .replaceAll("'",   "|'")
                        .replaceAll("\\[", "|[")
                        .replaceAll("]",   "|]")
        }

        void start() {
            teamcityReport("testSuiteStarted name='$name'")
        }

        void finish() {
            teamcityReport("testSuiteFinished name='$name'")
        }
    }
}
