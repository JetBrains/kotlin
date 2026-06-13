import org.gradle.jvm.toolchain.JavaLauncher
import org.jetbrains.kotlin.build.androidsdkprovisioner.ProvisioningType
import org.jetbrains.kotlin.testFederation.SmokeTestConfig
import org.jetbrains.kotlin.testFederation.TemporaryTestFederationApi
import org.jetbrains.kotlin.testFederation.smokeTestConfig

plugins {
    kotlin("jvm")
    id("android-sdk-provisioner")
    id("project-tests-convention")
    id("test-inputs-check")
}

dependencies {
    testImplementation(project(":core:descriptors"))
    testImplementation(project(":core:descriptors.jvm"))
    testImplementation(project(":compiler:util"))
    testImplementation(project(":compiler:cli"))
    testImplementation(project(":compiler:frontend"))
    testImplementation(project(":compiler:backend"))
    testImplementation(project(":compiler:incremental-compilation-impl"))
    testImplementation(project(":compiler:frontend.java"))

    testImplementation(kotlinStdlib())
    testImplementation(testFixtures(project(":compiler:tests-common")))
    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.core.jvm)
    testImplementation(testFixtures(project(":compiler:test-infrastructure")))
    testImplementation(testFixtures(project(":compiler:test-infrastructure-utils")))
    testImplementation(testFixtures(project(":compiler:tests-compiler-utils")))
    testImplementation(testFixtures(project(":compiler:tests-common-new")))

    testImplementation(jpsModel())

    testRuntimeOnly(intellijCore())
    testRuntimeOnly(commonDependency("org.jetbrains.intellij.deps.jna:jna"))

    testImplementation(libs.junit.platform.launcher)
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
    testTask(jUnitMode = JUnitMode.JUnit4) {
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

        if (project.hasProperty("teamcity") || project.hasProperty("kotlin.test.android.teamcity")) {
            systemProperty("kotlin.test.android.teamcity", true)
        }

        project.findProperty("kotlin.test.android.path.filter")?.let {
            systemProperty("kotlin.test.android.path.filter", it.toString())
        }

        androidSdkProvisioner {
            provideToThisTaskAsSystemProperty(ProvisioningType.SDK_WITH_EMULATOR)
        }

        @OptIn(TemporaryTestFederationApi::class)
        smokeTestConfig = SmokeTestConfig.Disabled

        testInputsCheck {
            with(extraPermissions) {
                add("permission java.util.PropertyPermission \"kotlin.test.android.path.filter\", \"read,write\";")
            }
        }

        testData(project(":compiler").isolated, "testData/codegen/box")
        testData(project(":compiler").isolated, "testData/codegen/boxJvm")
        testData(project(":compiler").isolated, "testData/codegen/boxInline")

        addDirectoryProperty(project.layout.projectDirectory.dir("android-module").asFile, "kotlin.test.android.androidModule")
        addDirectoryProperty(rootProject.layout.projectDirectory.dir("gradle/wrapper").asFile, "kotlin.test.android.gradleWrapper")
        addFileProperty(rootProject.layout.projectDirectory.file("gradlew"), "kotlin.test.android.gradlew")
        addFileProperty(rootProject.layout.projectDirectory.file("gradlew.bat"), "kotlin.test.android.gradlewBat")
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
) {
}
