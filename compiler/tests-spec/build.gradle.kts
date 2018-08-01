import java.util.regex.Pattern

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testRuntime(intellijDep())
    testCompile(projectTests(":compiler"))
    testCompile(projectDist(":kotlin-script-runtime"))
    testCompile(projectDist(":kotlin-stdlib"))
    testCompile(projectDist(":kotlin-test:kotlin-test-jvm"))
}

sourceSets {
    "main" { }
    "test" { projectDefault() }
}

projectTest {
    workingDir = rootDir
}

val generateTests by generator("org.jetbrains.kotlin.generators.tests.GenerateCompilerSpecTestsKt")

testsJar()

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
