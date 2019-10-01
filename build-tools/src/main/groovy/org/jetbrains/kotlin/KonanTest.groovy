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

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecResult

import javax.inject.Inject
import java.nio.file.Paths
import java.util.function.Function
import java.util.regex.Pattern

abstract class OldKonanTest extends JavaExec {
    public boolean inDevelopersRun = false

    public String source
    def platformManager = project.rootProject.platformManager
    def target = platformManager.targetManager(project.testTarget).target
    def dist = project.rootProject.file(project.findProperty("org.jetbrains.kotlin.native.home") ?:
            project.findProperty("konan.home") ?: "dist")
    def dependenciesDir = project.rootProject.dependenciesDir
    def konancDriver = project.isWindows() ? "konanc.bat" : "konanc"
    def konanc = new File("${dist.canonicalPath}/bin/$konancDriver").absolutePath
    def outputSourceSetName = "testOutputLocal"
    def enableKonanAssertions = true
    String outputDirectory = null
    String goldValue = null
    // Checks test's output against gold value and returns true if the output matches the expectation
    Function<String, Boolean> outputChecker = { str -> (goldValue == null || goldValue == str) }
    boolean printOutput = true
    String testData = null
    int expectedExitStatus = 0
    List<String> arguments = null
    List<String> flags = null

    boolean multiRuns = false
    List<List<String>> multiArguments = null

    boolean enabled = true
    boolean expectedFail = false
    boolean run = true
    boolean compilerMessages = false

    void setDisabled(boolean value) {
        this.enabled = !value
    }

    void setExpectedFail(boolean value) {
        this.expectedFail = value
    }

    // Uses directory defined in $outputSourceSetName source set.
    // If such source set doesn't exist, uses temporary directory.
    void createOutputDirectory() {
        if (outputDirectory != null) {
            return
        }
        def outputSourceSet = project.findProperty(getOutputSourceSetName())
        if (outputSourceSet != null) {
            outputDirectory = outputSourceSet.absolutePath + "/$name"
            project.file(outputDirectory).mkdirs()
        } else {
            outputDirectory = getTemporaryDir().absolutePath
        }
    }

    OldKonanTest() {
        // We don't build the compiler if a custom dist path is specified.
        if (!project.ext.useCustomDist) {
            dependsOn(project.rootProject.tasks['dist'])
            if (project.testTarget) {
                // if a test_target property is set then tests should depend on a crossDist
                // otherwise runtime components would not be build for a target
                dependsOn(project.rootProject.tasks["${target}CrossDist"])
            }
        }
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
            classpath = project.fileTree("$dist.canonicalPath/konan/lib/") {
                include '*.jar'
            }
            jvmArgs "-Dkonan.home=${dist.canonicalPath}", "-Xmx4G",
                    "-Djava.library.path=${dist.canonicalPath}/konan/nativelib"
            enableAssertions = true
            def sources = File.createTempFile(name,".lst")
            sources.deleteOnExit()
            def sourcesWriter = sources.newWriter()
            filesToCompile.each { f ->
                sourcesWriter.write(f.chars().any { Character.isWhitespace(it) }
                        ? "\"${f.replace("\\", "\\\\")}\"\n" // escape file name
                        : "$f\n")
            }
            sourcesWriter.close()
            args = ["-output", output,
                    "@${sources.absolutePath}",
                    *moreArgs,
                    *project.globalTestArgs]
            if (project.testTarget) {
                args "-target", target.visibleName
            }
            if (enableKonanAssertions) {
                args "-ea"
            }
            if (project.hasProperty("test_verbose")) {
                println("Files to compile: $filesToCompile")
                println(args)
            }
            standardOutput = log
            errorOutput = log
            super.exec()
        } finally {
            def logString = log.toString("UTF-8")
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

    // TODO refactor
    List<String> buildCompileList() {
        def result = []
        def filePattern = ~/(?m)\/\/\s*FILE:\s*(.*)$/
        def srcFile = project.file(source)
        def srcText = removeDiagnostics(srcFile.text)
        def matcher = filePattern.matcher(srcText)

        if (srcText.contains('// WITH_COROUTINES')) {
            def coroutineHelpersFileName = "$outputDirectory/helpers.kt"
            createFile(coroutineHelpersFileName, CoroutineTestUtilKt.createTextForHelpers(true))
            result.add(coroutineHelpersFileName)
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
        Paths.get(file).with {
            getParent().toFile().with {
                if (!exists()) { mkdirs() }
            }
            write(text)
        }
    }

    OutputStream out

    @TaskAction
    void executeTest() {
        if (!enabled) {
            println "Test is disabled: $name"
            return
        }

        createOutputDirectory()
        def program = buildExePath()
        def suffix = target.family.exeSuffix
        def exe = "$program.$suffix"

        compileTest(buildCompileList(), program)

        if (!run) {
            println "to be executed manually: $exe"
            return
        }
        println "execution: $exe"

        def compilerMessagesText = compilerMessages ? project.file("${program}.compilation.log").getText('UTF-8') : ""

        out = new ByteArrayOutputStream()
        //TODO Add test timeout

        def times = multiRuns ? multiArguments.size() : 1

        def exitCodeMismatch = false
        for (int i = 0; i < times; i++) {
            ExecResult execResult = project.execute {

                commandLine exe

                if (arguments != null) {
                    args arguments
                }
                if (multiRuns && multiArguments[i] != null) {
                    args multiArguments[i]
                }
                if (testData != null) {
                    standardInput = new ByteArrayInputStream(testData.bytes)
                }
                standardOutput = out

                ignoreExitValue = true
            }

            exitCodeMismatch |= execResult.exitValue != expectedExitStatus
            if (exitCodeMismatch) {
                def message = "Expected exit status: $expectedExitStatus, actual: ${execResult.exitValue}"
                if (this.expectedFail) {
                    println("Expected failure. $message")
                } else {
                    throw new TestFailedException("Test failed on iteration $i. $message\n ${out.toString("UTF-8")}")
                }
            }
        }
        def result = compilerMessagesText + out.toString("UTF-8")
        if (printOutput) {
            println(result)
        }
        result = result.replace(System.lineSeparator(), "\n")
        def goldValueMismatch = !outputChecker.apply(result)
        if (goldValueMismatch) {
            def message
            if (goldValue != null) {
                message = "Expected output: $goldValue, actual output: $result"
            } else {
                message = "Actual output doesn't match output checker: $result"
            }
            if (this.expectedFail) {
                println("Expected failure. $message")
            } else {
                throw new TestFailedException("Test failed. $message")
            }
        }

        if (!exitCodeMismatch && !goldValueMismatch && this.expectedFail) println("Unexpected pass")
    }
}

class RunExternalTestGroup extends OldKonanTest {
    /**
     * overrides [KonanTest::inDevelopersRun] used in [:backend.native:tests:sanity]
     */
    public def inDevelopersRun = false

    /**
     * If true, the test executable will be built in two stages:
     * 1. Build a klibrary from sources.
     * 2. Build a final executable from this klibrary.
     */
    @Input
    public def enableTwoStageCompilation = false

    def groupDirectory = "."
    def outputSourceSetName = "testOutputExternal"
    String filter = project.findProperty("filter")
    def testGroupReporter = new KonanTestGroupReportEnvironment(project)

    RunExternalTestGroup() {
    }

    @Override
    List<String> buildCompileList() {
        // Already build by the previous step
        return null
    }

    void parseLanguageFlags() {
        def text = project.file(source).text
        def languageSettings = findLinesWithPrefixesRemoved(text, "// !LANGUAGE: ")
        if (languageSettings.size() != 0) {
            languageSettings.forEach { line ->
                line.split(" ").toList().forEach { flags.add("-XXLanguage:$it") }
            }
        }

        def experimentalSettings = findLinesWithPrefixesRemoved(text, "// !USE_EXPERIMENTAL: ")
        if (experimentalSettings.size() != 0) {
            experimentalSettings.forEach { line ->
                line.split(" ").toList().forEach { flags.add("-Xuse-experimental=$it") }
            }
        }
    }

    static String markMutableObjects(String text) {
        def lines = text.readLines()
        def result = new ArrayList<String>(lines.size())
        lines.forEach { line ->
            // FIXME: find only those who has vars inside
            // Find object declarations and companion objects
            if (line.matches("\\s*(private|public|internal)?\\s*object [a-zA-Z_][a-zA-Z0-9_]*\\s*.*")
                    || line.matches("\\s*(private|public|internal)?\\s*companion object.*")) {
                result += "@kotlin.native.ThreadLocal"
            }
            result += line
        }
        return result.join(System.lineSeparator())
    }

    String insertInTextAfter(String text, String insert, String after) {
        def begin = text.indexOf(after)
        if (begin != -1) {
            def end = text.indexOf("\n", begin)
            text = text.substring(0, end) + insert + text.substring(end)
        } else {
            text = insert + text
        }
        return text
    }

    List<String> createTestFiles() {
        def identifier = /[a-zA-Z_][a-zA-Z0-9_]/
        def fullQualified = /[a-zA-Z_][a-zA-Z0-9_.]/
        def importRegex = /(?m)^\s*import\s+/

        def packagePattern = ~/(?m)^\s*package\s+(${fullQualified}*)/
        def boxPattern = ~/(?m)fun\s+box\s*\(\s*\)/
        def classPattern = ~/.*(class|object|enum)\s+(${identifier}*).*/

        def sourceName = "_" + normalize(project.file(source).name)
        def packages = new LinkedHashSet<String>()
        def imports = []

        def result = super.buildCompileList()
        for (String filePath : result) {
            def text = project.file(filePath).text
            if (text.contains('COROUTINES_PACKAGE')) {
                text = text.replace('COROUTINES_PACKAGE', 'kotlin.coroutines')
            }
            def pkg = null
            if (text =~ packagePattern) {
                pkg = (text =~ packagePattern)[0][1]
                packages.add(pkg)
                pkg = "$sourceName.$pkg"
                text = text.replaceFirst(packagePattern, "package $pkg")
            } else {
                pkg = sourceName
                text = insertInTextAfter(text, "\npackage $pkg\n", "@file:Suppress")
            }
            if (text =~ boxPattern) {
                imports.add("${pkg}.*")
            }

            // Find mutable objects that should be marked as ThreadLocal
            if (filePath != "$outputDirectory/helpers.kt") {
                text = markMutableObjects(text)
            }

            createFile(filePath, text)
        }
        // TODO: optimize files writes
        for (String filePath : result) {
            def text = project.file(filePath).text
            // Find if there are any imports in the file
            def matcher = (text =~ ~/${importRegex}(${fullQualified}*)/)
            if (matcher) {
                // Prepend package name to found imports
                for (int i = 0; i < matcher.count; i++) {
                    String importStatement = matcher[i][1]
                    def subImport = importStatement.with {
                        int dotIdx = indexOf('.')
                        dotIdx > 0 ? substring(0, dotIdx) : it
                    }
                    if (packages.contains(subImport)) {
                        // add only to those who import packages from the test files
                        text = text.replaceFirst(~/${importRegex}${Pattern.quote(importStatement)}/,
                                "import $sourceName.$importStatement")
                    } else if (text =~ classPattern) {
                        // special case for import from the local class
                        def clsMatcher = (text =~ classPattern)
                        for (int j = 0; j < clsMatcher.count; j++) {
                            def cl = (text =~ classPattern)[j][2]
                            if (subImport == cl) {
                                text = text.replaceFirst(~/${importRegex}${Pattern.quote(importStatement)}/,
                                        "import $sourceName.$importStatement")
                            }
                        }
                    }
                }
            } else if (packages.empty) {
                // Add import statement after package
                def pkg = null
                if (text =~ packagePattern) {
                    pkg = 'package ' + (text =~ packagePattern)[0][1]
                    text = text.replaceFirst(packagePattern, '')
                }
                text = insertInTextAfter(text, (pkg ? "\n$pkg\n" : "") + "import $sourceName.*\n", "@file:Suppress")
            }
            // now replace all package usages in full qualified names
            def res = ""                      // result
            def vars = new HashSet<String>()  // variables that has the same name as a package
            text.eachLine { line ->
                packages.each { pkg ->
                    // line contains val or var declaration or function parameter declaration
                    if ((line =~ ~/va(l|r) *$pkg *\=/) || (line =~ ~/fun .*\(\n?\s*$pkg:.*/)) {
                        vars.add(pkg)
                    }
                    if (line.contains("$pkg.") && ! (line =~ packagePattern || line =~ importRegex)
                            && ! vars.contains(pkg)) {
                        def idx = 0
                        while ((idx = line.indexOf(pkg, idx)) >= 0) {
                            if (!Character.isJavaIdentifierPart(line.charAt(idx - 1))) {
                                line = line.substring(0, idx) + "$sourceName.$pkg" + line.substring(idx + pkg.length())
                                idx += sourceName.length() + pkg.length() + 1
                            } else {
                                idx += pkg.length()
                            }
                        }
                    }
                }
                res += "$line\n"
            }
            createFile(filePath, res)
        }
        createLauncherFile("$outputDirectory/_launcher.kt", imports)
        result.add("$outputDirectory/_launcher.kt")
        return result
    }

    String normalize(String name) {
        name.replace('.kt', '')
                .replace('-','_')
                .replace('.', '_')
    }

    /**
     * There are tests that require non-trivial 'package foo' in test launcher.
     */
    void createLauncherFile(String file, List<String> imports) {
        StringBuilder text = new StringBuilder()
        def pack = normalize(project.file(source).name)
        text.append("package _$pack\n")
        for (v in imports) {
            text.append("import ").append(v).append('\n')
        }

        text.append(
"""
import kotlin.test.Test

@Test
fun runTest() {
    @Suppress("UNUSED_VARIABLE")
    val result = box()
    if (result != "OK") throw AssertionError("Test failed with: " + result)
    print(result)
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

    static def excludeList = [
            "build/external/compiler/codegen/boxInline/anonymousObject/kt8133.kt"  // KT-34066
    ]

    boolean isEnabledForNativeBackend(String fileName) {
        def text = project.file(fileName).text

        if (excludeList.contains(fileName.replace(File.separator, "/"))) return false

        if (findLinesWithPrefixesRemoved(text, "// WITH_REFLECT").size() != 0) return false

        def languageSettings = findLinesWithPrefixesRemoved(text, '// !LANGUAGE: ')
        if (!languageSettings.empty) {
            def settings = languageSettings.first()
            if (settings.contains('-ProperIeee754Comparisons') ||  // K/N supports only proper IEEE754 comparisons
                    settings.contains('+NewInference') ||          // New inference is not implemented
                    settings.contains('-ReleaseCoroutines') ||     // only release coroutines
                    settings.contains('-DataClassInheritance')) {  // old behavior is not supported
                return false
            }
        }

        def version = findLinesWithPrefixesRemoved(text, '// LANGUAGE_VERSION: ')
        if (version.size() != 0 && !version.contains("1.3")) {
            // Support tests for 1.3 and exclude 1.2
            return false
        }

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
            // No ignored backends. Check if test is targeted to FULL_JDK or has JVM_TARGET set
            if (!findLinesWithPrefixesRemoved(text, "// FULL_JDK").isEmpty()) { return false }
            if (!findLinesWithPrefixesRemoved(text, "// JVM_TARGET:").isEmpty()) { return false }
            return true
        }
    }

    @Override
    void compileTest(List<String> filesToCompile, String exe) {
        // An executable should be already compiled
    }

    @Override
    String buildExePath() {
        def outputDir
        def outputSourceSet = project.findProperty(getOutputSourceSetName())
        if (outputSourceSet != null) {
            outputDir  = outputSourceSet.absolutePath+ "/$name"
        } else {
            outputDir = getTemporaryDir().absolutePath
        }
        return "$outputDir/program.tr"
    }

    @TaskAction
    @Override
    void executeTest() {
        createOutputDirectory()
        def outputRootDirectory = outputDirectory

        // Form the test list.
        List<File> ktFiles = project.file(groupDirectory)
                .listFiles({
                    it.isFile() && it.name.endsWith(".kt")
                } as FileFilter)
        if (filter != null) {
            def pattern = ~filter
            ktFiles = ktFiles.findAll {
                it.name =~ pattern
            }
        }

        testGroupReporter.suite(name) { suite ->
            // Build tests in the group
            flags = (flags ?: []) + "-tr"
            def compileList = []
            ktFiles.each {
                source = project.relativePath(it)
                if (isEnabledForNativeBackend(source)) {
                    // Create separate output directory for each test in the group.
                    outputDirectory = outputRootDirectory + "/${it.name}"
                    project.file(outputDirectory).mkdirs()
                    parseLanguageFlags()
                    compileList.addAll(createTestFiles())
                }
            }
            compileList.add(project.file("testUtils.kt").absolutePath)
            compileList.add(project.file("helpers.kt").absolutePath)
            try {
                def exePath = buildExePath()
                if (enableTwoStageCompilation) {
                    // Two-stage compilation.
                    def klibPath = "${exePath}.klib"
                    runCompiler(compileList, klibPath, flags + ["-p", "library"])
                    runCompiler([], exePath, flags + ["-Xinclude=$klibPath"])
                } else {
                    // Regular compilation.
                    runCompiler(compileList, exePath, flags)
                }
            } catch (Exception ex) {
                project.logger.quiet("ERROR: Compilation failed for test suite: $name with exception", ex)
                project.logger.quiet("The following files were unable to compile:")
                ktFiles.each { project.logger.quiet(it.name) }
                suite.abort(ex, ktFiles.size())
                throw new RuntimeException("Compilation failed", ex)
            }

            // Run the tests.
            def currentResult = null
            outputDirectory = outputRootDirectory
            arguments = (arguments ?: []) + "--ktest_logger=SILENT"
            ktFiles.each { file ->
                source = project.relativePath(file)
                def savedArgs = arguments
                arguments += "--ktest_filter=_${normalize(file.name)}.*"
                use(KonanTestSuiteReportKt) {
                    project.logger.quiet("TEST: $file.name (done: $testGroupReporter.statistics.total/${ktFiles.size()}, passed: $testGroupReporter.statistics.passed, skipped: $testGroupReporter.statistics.skipped)")
                }
                if (isEnabledForNativeBackend(source)) {
                    suite.executeTest(file.name) {
                       project.logger.quiet(source)
                       super.executeTest()
                    }
                } else {
                    suite.skipTest(file.name)
                }
                arguments = savedArgs
            }
        }
    }
}
