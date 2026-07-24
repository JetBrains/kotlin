import org.gradle.jvm.toolchain.JavaLauncher
import org.jetbrains.kotlin.build.androidsdkprovisioner.ProvisioningType
import org.jetbrains.kotlin.testFederation.SmokeTestConfig
import org.jetbrains.kotlin.testFederation.TemporaryTestFederationApi
import org.jetbrains.kotlin.testFederation.smokeTestConfig

plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("android-sdk-provisioner")
    id("project-tests-convention")
    id("test-inputs-check-v2")
}

dependencies {
    testImplementation(project(":core:descriptors.jvm"))
    testImplementation(project(":compiler:util"))
    testImplementation(project(":compiler:cli"))
    testImplementation(project(":compiler:frontend"))
    testImplementation(project(":compiler:backend"))

    testImplementation(testFixtures(project(":compiler:tests-common")))

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)

    testImplementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.core.jvm)
    testImplementation(testFixtures(project(":compiler:test-infrastructure")))
    testImplementation(testFixtures(project(":compiler:test-infrastructure-utils")))
    testImplementation(testFixtures(project(":compiler:tests-compiler-utils")))
    testImplementation(testFixtures(project(":compiler:tests-common-new")))


    testRuntimeOnly(intellijCore())
    testRuntimeOnly(commonDependency("org.jetbrains.intellij.deps.jna:jna"))
}

sourceSets {
    "main" { }
    "test" { projectDefault() }
}

optInToK1Deprecation()

val acceptAndroidSdkLicenses = with(androidSdkProvisioner) {
    project.registerAcceptLicensesTask()
}

abstract class JdkHomeArgumentProvider : CommandLineArgumentProvider {
    @get:Nested
    abstract val javaLauncher: Property<JavaLauncher>

    override fun asArguments(): Iterable<String> =
        listOf("-Dorg.gradle.java.home=${javaLauncher.get().metadata.installationPath.asFile.absolutePath}")
}

projectTests {
    testTask(javaLauncher = JdkMajorVersion.JDK_17_0) {
        develocity {
            testRetry.maxRetries.set(0)
        }

        testLogging {
            showStandardStreams = true
        }

        dependsOn(acceptAndroidSdkLicenses)
        environment("kotlin.tests.android.timeout", "45")

        val jdkHomeProvider = objects.newInstance<JdkHomeArgumentProvider>()
        jdkHomeProvider.javaLauncher.set(project.getToolchainLauncherFor(JdkMajorVersion.JDK_17_0))
        jvmArgumentProviders.add(jdkHomeProvider)

        if (project.kotlinBuildProperties.isTeamcityBuild.get() || project.providers.gradleProperty("kotlin.test.android.teamcity").isPresent) {
            systemProperty("kotlin.test.android.teamcity", true)
        }

        for (propertyName in listOf(
            "kotlin.test.android.path.filter",
            "kotlin.test.android.compilation.parallelism",
            "kotlin.test.android.avd.systemImage",
        )) {
            project.providers.gradleProperty(propertyName).orNull?.let {
                systemProperty(propertyName, it)
            }
        }

        androidSdkProvisioner {
            provideToThisTaskAsSystemProperty(ProvisioningType.SDK_WITH_EMULATOR)
        }

        @OptIn(TemporaryTestFederationApi::class)
        smokeTestConfig = SmokeTestConfig.Disabled


        testData(project(":compiler").isolated, "testData/codegen/box")
        testData(project(":compiler").isolated, "testData/codegen/boxJvm")
        testData(project(":compiler").isolated, "testData/codegen/boxInline")

        addDirectoryProperty(project.layout.projectDirectory.dir("android-module").asFile, "kotlin.test.android.androidModule")
        addDirectoryProperty(rootProject.isolated.projectDirectory.dir("gradle/wrapper").asFile, "kotlin.test.android.gradleWrapper")
        addFileProperty(rootProject.isolated.projectDirectory.file("gradlew"), "kotlin.test.android.gradlew")
        addFileProperty(rootProject.isolated.projectDirectory.file("gradlew.bat"), "kotlin.test.android.gradlewBat")
    }

    withJvmStdlibAndReflect()
    withTestJar()
    withScriptRuntime()
    withMockJdkAnnotationsJar()
    withMockJdkRuntime()
}

val generateAndroidTests by generator(
    "org.jetbrains.kotlin.android.tests.CodegenTestsOnAndroidGenerator",
    testSourceSet,
    inputKind = GeneratorInputKind.RuntimeClasspath,
    registerInAggregateGenerateSources = false,
) {
}
