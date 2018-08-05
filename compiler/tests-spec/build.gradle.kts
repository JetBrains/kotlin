import java.util.regex.Pattern
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.google.gson.reflect.TypeToken
import java.util.regex.Matcher

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testCompile(projectTests(":compiler"))
}

sourceSets {
    "main" { }
    "test" { projectDefault() }
}

projectTest {
    workingDir = rootDir
}

val generateTests by generator("org.jetbrains.kotlin.generators.tests.GenerateCompilerSpecTestsKt")

tasks.create("printSpecTestsStatistic").doLast {
    val testDataDir = "./testData"
    val specTestAreas = listOf("diagnostics", "psi", "codegen")
    val integerRegex = "[1-9]\\d*"
    val testPathRegex =
        "s-(?<sectionNumber>(?:$integerRegex)(?:\\.$integerRegex)*)_(?<sectionName>[\\w-]+)/p-(?<paragraphNumber>$integerRegex)/(?<testType>pos|neg)/(?<sentenceNumber>$integerRegex)\\.(?<testNumber>$integerRegex)\\.kt$"

    abstract class StatElement {
        var counter = 0
        abstract val elements: MutableMap<*, out StatElement>?
        open fun increment() {
            counter++
        }
    }

    class TestTypeStat(private val paragraph: StatElement): StatElement() {
        override val elements = null
        override fun increment() {
            super.increment()
            paragraph.increment()
        }
    }

    class ParagraphStat(private val section: StatElement): StatElement() {
        override val elements = sortedMapOf<String, TestTypeStat>()
        override fun increment() {
            super.increment()
            section.increment()
        }
    }

    class SectionStat(private val area: StatElement): StatElement() {
        override val elements = sortedMapOf<Int, ParagraphStat>()
        override fun increment() {
            super.increment()
            area.increment()
        }
    }

    class AreaStat: StatElement() {
        override val elements = sortedMapOf<String, SectionStat>()
    }

    fun printStat(statistic: MutableMap<String, AreaStat>) {
        println("--------------------------------------------------")
        println("SPEC TESTS STATISTIC")
        println("--------------------------------------------------")

        statistic.forEach {
            println("${it.key.toUpperCase()}: ${it.value.counter} tests")

            it.value.elements.forEach {
                println("  ${it.key.toUpperCase()}: ${it.value.counter} tests")

                it.value.elements.forEach {
                    val testTypes = mutableListOf<String>()

                    it.value.elements.forEach {
                        testTypes.add("${it.key}: ${it.value.counter}")
                    }

                    println("    PARAGRAPH ${it.key}: ${it.value.counter} tests (${testTypes.joinToString(", ")})")
                }
            }
        }

        println("--------------------------------------------------")
    }

    fun incrementStatCounters(testAreaStats: AreaStat, sectionName: String, paragraphNumber: Int, testType: String) {
        val section = testAreaStats.elements.computeIfAbsent(sectionName) { SectionStat(testAreaStats) }
        val paragraph = section.elements.computeIfAbsent(paragraphNumber) { ParagraphStat(section) }

        paragraph.elements.computeIfAbsent(testType) { TestTypeStat(paragraph) }.increment()
    }

    val statistic = mutableMapOf<String, AreaStat>()

    specTestAreas.forEach {
        val specTestArea = it
        val specTestsPath = "$testDataDir/$specTestArea"

        statistic[specTestArea] = AreaStat()

        File(specTestsPath).walkTopDown().forEach areaTests@{
            if (!it.isFile || it.extension != "kt") {
                return@areaTests
            }

            val testInfoMatcher = Pattern.compile(testPathRegex).matcher(it.path)

            if (!testInfoMatcher.find()) {
                return@areaTests
            }

            val sectionNumber = testInfoMatcher.group("sectionNumber")
            val sectionName = testInfoMatcher.group("sectionName")
            val paragraphNumber = testInfoMatcher.group("paragraphNumber").toInt()
            val testType = testInfoMatcher.group("testType")
            val section = "$sectionNumber $sectionName"

            incrementStatCounters(statistic[specTestArea]!!, section, paragraphNumber, testType)
        }
    }

    printStat(statistic)
}

tasks.create("generateJsonTestsMap").doLast {
    val testDataDir = "./testData"
    val outDir = "./out"
    val outFilename = "testsMap.json"

    val specTestAreas = listOf("diagnostics", "psi", "codegen")
    val integerRegex = "[1-9]\\d*"
    val sectionFolderRegex = "s-(?<sectionNumber>(?:$integerRegex)(?:\\.$integerRegex)*)_(?<sectionName>[\\w-]+)"
    val testPathRegex =
        "^.*?/(?<testArea>${specTestAreas.joinToString("|")})/$sectionFolderRegex/p-(?<paragraphNumber>$integerRegex)/(?<testType>pos|neg)/(?<sentenceNumber>$integerRegex)\\.(?<testNumber>$integerRegex)\\.kt$"
    val testUnexpectedBehaviour = "(?:\n\\s*(?<unexpectedBehaviour>UNEXPECTED BEHAVIOUR))"
    val testIssues = "(?:\n\\s*ISSUES:\\s*(?<issues>(KT-[1-9]\\d*)(,\\s*KT-[1-9]\\d*)*))"
    val testContentRegex =
        "\\/\\*\\s+KOTLIN SPEC TEST \\((?<testType>POSITIVE|NEGATIVE)\\)\\s+SECTION (?<sectionNumber>(?:$integerRegex)(?:\\.$integerRegex)*):\\s*(?<sectionName>.*?)\\s+PARAGRAPH:\\s*(?<paragraphNumber>$integerRegex)\\s+SENTENCE\\s*(?<sentenceNumber>$integerRegex):\\s*(?<sentence>.*?)\\s+NUMBER:\\s*(?<testNumber>$integerRegex)\\s+DESCRIPTION:\\s*(?<description>.*?)$testUnexpectedBehaviour?$testIssues?\\s+\\*\\/\\s+"
    val testCaseInfo =
        "(?:(?:\\/\\*\n\\s*)|(?:\\/\\/\\s*))CASE DESCRIPTION:\\s*(?<description>.*?)$testUnexpectedBehaviour?$testIssues?\n(\\s\\*\\/)?"

    val stringListType = object : TypeToken<List<String>>() {}.getType()

    fun addJsonIfNotExist(element: JsonObject, key: String): JsonObject {
        if (!element.has(key)) element.add(key, JsonObject())
        return element.get(key).asJsonObject
    }

    fun addInfoToTestElement(testElement: JsonObject, testElementInfoMatcher: Matcher): JsonObject {
        val unexpectedBehaviour = testElementInfoMatcher.group("unexpectedBehaviour") != null
        val issues = testElementInfoMatcher.group("issues")?.split(Regex(",\\s*"))

        testElement.addProperty("description", testElementInfoMatcher.group("description"))
        if (unexpectedBehaviour) testElement.addProperty("unexpectedBehaviour", unexpectedBehaviour)
        if (issues !== null) testElement.add("issues", Gson().toJsonTree(issues, stringListType))

        return testElement
    }

    fun getTestCasesInfo(testCaseInfoMatcher: Matcher, testInfoMatcher: Matcher): JsonArray {
        val testCases = JsonArray()

        while (testCaseInfoMatcher.find()) {
            testCases.add(addInfoToTestElement(JsonObject(), testCaseInfoMatcher))
        }

        return testCases
    }

    val testsMap = JsonObject()
    val gson = Gson()

    File(testDataDir).walkTopDown().forEach {
        val testInfoByPathMatcher = Pattern.compile(testPathRegex).matcher(it.path)
        if (!testInfoByPathMatcher.find()) return@forEach

        val sectionElement = addJsonIfNotExist(testsMap, testInfoByPathMatcher.group("sectionName"))
        val paragraphElement = addJsonIfNotExist(sectionElement, testInfoByPathMatcher.group("paragraphNumber"))
        val sentenceElement = addJsonIfNotExist(paragraphElement, testInfoByPathMatcher.group("sentenceNumber"))
        val testAreaElement = addJsonIfNotExist(sentenceElement, testInfoByPathMatcher.group("testArea"))
        val testTypeElement = addJsonIfNotExist(testAreaElement, testInfoByPathMatcher.group("testType"))
        val testNumberElement = addJsonIfNotExist(testTypeElement, testInfoByPathMatcher.group("testNumber"))

        val testFileContent = File(it.path).readText()
        val testInfoByContentMatcher = Pattern.compile(testContentRegex).matcher(testFileContent)
        val testCaseInfoMatcher = Pattern.compile(testCaseInfo).matcher(testFileContent)

        testInfoByContentMatcher.find()

        addInfoToTestElement(testNumberElement, testInfoByContentMatcher)

        val testCases = getTestCasesInfo(testCaseInfoMatcher, testInfoByContentMatcher)
        if (testCases.size() != 0) testNumberElement.add("cases", testCases)
    }

    File(outDir).mkdir()
    File("$outDir/$outFilename").writeText(testsMap.toString())
}
