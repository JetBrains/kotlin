@file:Suppress("UnstableApiUsage")

import org.gradle.jvm.tasks.Jar
import org.jetbrains.gradle.ext.ActionDelegationConfig
import org.jetbrains.gradle.ext.JUnit
import org.jetbrains.gradle.ext.RecursiveArtifact
import org.jetbrains.gradle.ext.TopLevelArtifact
import org.jetbrains.kotlin.ideaExt.*


val distDir: String by extra
val ideaSandboxDir: File by extra
val ideaSdkPath: String
    get() = rootProject.ideaHomePathForTests().absolutePath

fun MutableList<String>.addModularizedTestArgs(prefix: String, path: String, additionalParameters: Map<String, String>, benchFilter: String?) {
    add("-${prefix}fir.bench.prefix=$path")
    add("-${prefix}fir.bench.jps.dir=$path/test-project-model-dump")
    add("-${prefix}fir.bench.passes=1")
    add("-${prefix}fir.bench.dump=true")
    for ((name, value) in additionalParameters) {
        add("-$prefix$name=$value")
    }
    if (benchFilter != null) {
        add("-${prefix}fir.bench.filter=$benchFilter")
    }
}

fun generateVmParametersForJpsConfiguration(path: String, additionalParameters: Map<String, String>, benchFilter: String?): String {
    val vmParameters = mutableListOf(
        "-ea",
        "-XX:+HeapDumpOnOutOfMemoryError",
        "-Xmx3600m",
        "-XX:+UseCodeCacheFlushing",
        "-XX:ReservedCodeCacheSize=128m",
        "-Djna.nosys=true",
        "-Didea.platform.prefix=Idea",
        "-Didea.is.unit.test=true",
        "-Didea.ignore.disabled.plugins=true",
        "-Didea.home.path=$ideaSdkPath",
        "-Didea.use.native.fs.for.win=false",
        "-Djps.kotlin.home=${File(distDir).absolutePath}/kotlinc",
        "-Duse.jps=true",
        "-Djava.awt.headless=true"
    )
    vmParameters.addModularizedTestArgs(prefix = "D", path = path, additionalParameters = additionalParameters, benchFilter = benchFilter)
    return vmParameters.joinToString(" ")
}

fun generateArgsForGradleConfiguration(path: String, additionalParameters: Map<String, String>, benchFilter: String?): String {
    val args = mutableListOf<String>()
    args.addModularizedTestArgs(prefix = "P", path = path, additionalParameters = additionalParameters, benchFilter = benchFilter)
    return args.joinToString(" ")
}

fun generateXmlContentForJpsConfiguration(name: String, testClassName: String, vmParameters: String): String {
    return """
        <component name="ProjectRunConfigurationManager">
          <configuration default="false" name="$name" type="JUnit" factoryName="JUnit" folderName="Modularized tests">
            <module name="kotlin.compiler.fir.modularized-tests.test" />
            <extension name="coverage">
              <pattern>
                <option name="PATTERN" value="org.jetbrains.kotlin.fir.*" />
                <option name="ENABLED" value="true" />
              </pattern>
            </extension>
            <option name="PACKAGE_NAME" value="org.jetbrains.kotlin.fir" />
            <option name="MAIN_CLASS_NAME" value="org.jetbrains.kotlin.fir.$testClassName" />
            <option name="METHOD_NAME" value="" />
            <option name="TEST_OBJECT" value="class" />
            <option name="VM_PARAMETERS" value="$vmParameters" />
            <option name="PARAMETERS" value="" />
            <option name="WORKING_DIRECTORY" value="${'$'}PROJECT_DIR${'$'}" />
            <envs>
              <env name="NO_FS_ROOTS_ACCESS_CHECK" value="true" />
              <env name="PROJECT_CLASSES_DIRS" value="out/test/org.jetbrains.kotlin.compiler.test" />
            </envs>
            <method v="2">
              <option name="Make" enabled="true" />
            </method>
          </configuration>
        </component>
    """.trimIndent()
}

fun generateXmlContentForGradleConfiguration(name: String, testClassName: String, vmParameters: String): String {
    return """
        <component name="ProjectRunConfigurationManager">
          <configuration default="false" name="$name" type="GradleRunConfiguration" factoryName="Gradle" folderName="Modularized tests">
            <ExternalSystemSettings>
              <option name="executionName" />
              <option name="externalProjectPath" value="${'$'}PROJECT_DIR${'$'}" />
              <option name="externalSystemIdString" value="GRADLE" />
              <option name="scriptParameters" value="--tests &quot;org.jetbrains.kotlin.fir.${testClassName}&quot; ${vmParameters}" />
              <option name="taskDescriptions">
                <list />
              </option>
              <option name="taskNames">
                <list>
                  <option value=":compiler:fir:modularized-tests:test" />
                </list>
              </option>
              <option name="vmOptions" value="" />
            </ExternalSystemSettings>
            <GradleScriptDebugEnabled>true</GradleScriptDebugEnabled>
            <method v="2" />
          </configuration>
        </component>
    """.trimIndent()
}

fun String.convertNameToRunConfigurationFile(prefix: String = ""): File {
    val fileName = prefix + replace("""[ -.\[\]]""".toRegex(), "_") + ".xml"
    return rootDir.resolve(".idea/runConfigurations/${fileName}")
}

fun generateJpsConfiguration(name: String, testClassName: String, path: String, additionalParameters: Map<String, String>, benchFilter: String?) {
    val vmParameters = generateVmParametersForJpsConfiguration(path, additionalParameters, benchFilter)
    val content = generateXmlContentForJpsConfiguration(
        name = name,
        testClassName = testClassName,
        vmParameters = vmParameters
    )
    name.convertNameToRunConfigurationFile("JPS").writeText(content)
}

fun generateGradleConfiguration(name: String, testClassName: String, path: String, additionalParameters: Map<String, String>, benchFilter: String?) {
    val vmParameters = generateArgsForGradleConfiguration(path, additionalParameters, benchFilter)
    val content = generateXmlContentForGradleConfiguration(
        name = name,
        testClassName = testClassName,
        vmParameters = vmParameters
    )
    name.convertNameToRunConfigurationFile().writeText(content)
}

data class Configuration(val path: String, val name: String, val additionalParameters: Map<String, String> = emptyMap()) {
    companion object {
        operator fun invoke(path: String?, name: String, additionalParameters: Map<String, String> = emptyMap()): Configuration? {
            return path?.let { Configuration(it, name, additionalParameters) }
        }
    }
}

val testDataPathList = listOfNotNull(
    Configuration(kotlinBuildProperties.pathToKotlinModularizedTestData, "Kotlin"),
    Configuration(kotlinBuildProperties.pathToIntellijModularizedTestData, "IntelliJ"),
    Configuration(kotlinBuildProperties.pathToYoutrackModularizedTestData, "YouTrack"),
    Configuration(kotlinBuildProperties.pathToSpaceModularizedTestData, "Space")
)

val generateMT = kotlinBuildProperties.generateModularizedConfigurations
val generateFP = kotlinBuildProperties.generateFullPipelineConfigurations

for ((path, projectName, additionalParameters) in testDataPathList) {
    rootProject.afterEvaluate {
        val configurations = mutableListOf<Pair<String, String?>>(
            "Full $projectName" to null
        )

        val jpsBuildEnabled = kotlinBuildProperties.isInJpsBuildIdeaSync

        for ((name, benchFilter) in configurations) {
            if (generateMT) {
                generateGradleConfiguration(
                    "[MT] $name",
                    "FirResolveModularizedTotalKotlinTest",
                    path,
                    additionalParameters,
                    benchFilter
                )
            }
            if (generateFP) {
                generateGradleConfiguration(
                    "[FP] $name",
                    "FullPipelineModularizedTest",
                    path,
                    additionalParameters,
                    benchFilter
                )
            }
            if (jpsBuildEnabled) {
                if (generateMT) {
                    generateJpsConfiguration(
                        "[MT-JPS] $name",
                        "FirResolveModularizedTotalKotlinTest",
                        path,
                        additionalParameters,
                        benchFilter
                    )
                }
                if (generateFP) {
                    generateJpsConfiguration(
                        "[FP-JPS] $name",
                        "FullPipelineModularizedTest",
                        path,
                        additionalParameters,
                        benchFilter
                    )
                }
            }
        }
    }
}
