@file:Suppress("UnstableApiUsage")

import org.gradle.jvm.tasks.Jar
import org.jetbrains.gradle.ext.ActionDelegationConfig
import org.jetbrains.gradle.ext.JUnit
import org.jetbrains.gradle.ext.RecursiveArtifact
import org.jetbrains.gradle.ext.TopLevelArtifact
import org.jetbrains.kotlin.ideaExt.*


val ideaPluginDir: File by extra
val ideaSandboxDir: File by extra
val ideaSdkPath: String
    get() = IntellijRootUtils.getIntellijRootDir(rootProject).absolutePath

fun MutableList<String>.addModularizedTestArgs(prefix: String, path: String, languageVersion: String, benchFilter: String?) {
    add("-${prefix}fir.bench.prefix=$path")
    add("-${prefix}fir.bench.jps.dir=$path/test-project-model-dump")
    add("-${prefix}fir.bench.passes=1")
    add("-${prefix}fir.bench.dump=true")
    add("-${prefix}fir.bench.language.version=$languageVersion")
    if (benchFilter != null) {
        add("-${prefix}fir.bench.filter=$benchFilter")
    }
}

fun generateVmParametersForJpsConfiguration(path: String, languageVersion: String, benchFilter: String?): String {
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
        "-Djps.kotlin.home=${ideaPluginDir.absolutePath}",
        "-Dkotlin.ni=" + if (rootProject.hasProperty("newInferenceTests")) "true" else "false",
        "-Duse.jps=true",
        "-Djava.awt.headless=true"
    )
    vmParameters.addModularizedTestArgs(prefix = "D", path = path, languageVersion = languageVersion, benchFilter = benchFilter)
    return vmParameters.joinToString(" ")
}

fun generateArgsForGradleConfiguration(path: String, languageVersion: String, benchFilter: String?): String {
    val args = mutableListOf<String>()
    args.addModularizedTestArgs(prefix = "P", path = path, languageVersion = languageVersion, benchFilter = benchFilter)
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

fun generateJpsConfiguration(name: String, testClassName: String, path: String, languageVersion: String, benchFilter: String?) {
    val vmParameters = generateVmParametersForJpsConfiguration(path, languageVersion, benchFilter)
    val content = generateXmlContentForJpsConfiguration(
        name = name,
        testClassName = testClassName,
        vmParameters = vmParameters
    )
    name.convertNameToRunConfigurationFile("JPS").writeText(content)
}

fun generateGradleConfiguration(name: String, testClassName: String, path: String, languageVersion: String, benchFilter: String?) {
    val vmParameters = generateArgsForGradleConfiguration(path, languageVersion, benchFilter)
    val content = generateXmlContentForGradleConfiguration(
        name = name,
        testClassName = testClassName,
        vmParameters = vmParameters
    )
    name.convertNameToRunConfigurationFile().writeText(content)
}

data class Configuration(val path: String, val name: String, val languageVersion: String) {
    companion object {
        operator fun invoke(path: String?, name: String, languageVersion: String): Configuration? {
            return path?.let { Configuration(it, name, languageVersion) }
        }
    }
}

val testDataPathList = listOfNotNull(
    Configuration(kotlinBuildProperties.pathToKotlinModularizedTestData, "Kotlin", "1.4"),
    Configuration(kotlinBuildProperties.pathToIntellijModularizedTestData, "IntelliJ", "1.4"),
    Configuration(kotlinBuildProperties.pathToYoutrackModularizedTestData, "YouTrack", "1.5")
)

val additionalConfigurationsWithFilter = mapOf(
    "Kotlin" to listOf(
        "Kotlin. All main modules" to ".*/main",
        "Kotlin. idea.main module" to ".*/idea/build/.*/main",
        "Kotlin. idea.test module" to ".*/idea/build/.*/test"
    )
)

for ((path, projectName, languageVersion) in testDataPathList) {
    rootProject.afterEvaluate {
        val configurations = mutableListOf<Pair<String, String?>>(
            "Full $projectName" to null
        )

        additionalConfigurationsWithFilter[projectName]?.let {
            configurations.addAll(it)
        }

        val jpsBuildEnabled = kotlinBuildProperties.isInJpsBuildIdeaSync

        for ((name, benchFilter) in configurations) {
            generateGradleConfiguration("[MT] $name", "FirResolveModularizedTotalKotlinTest", path, languageVersion, benchFilter)
            generateGradleConfiguration("[FP] $name", "FullPipelineModularizedTest", path, languageVersion, benchFilter)
            if (jpsBuildEnabled) {
                generateJpsConfiguration("[MT-JPS] $name", "FirResolveModularizedTotalKotlinTest", path, languageVersion, benchFilter)
                generateJpsConfiguration("[FP-JPS] $name", "FullPipelineModularizedTest", path, languageVersion, benchFilter)
            }
        }
    }
}
