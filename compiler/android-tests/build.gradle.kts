import org.jetbrains.kotlin.build.androidsdkprovisioner.ProvisioningType

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

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit4) {
        develocity {
            testRetry.maxRetries.set(0)
        }

        dependsOn(":dist")
        dependsOn(acceptAndroidSdkLicenses)
        val jdkHome = project.getToolchainJdkHomeFor(JdkMajorVersion.JDK_17_0)
        doFirst {
            environment("kotlin.tests.android.timeout", "45")
            environment("JAVA_HOME", jdkHome.get())
        }

        if (project.hasProperty("teamcity") || project.hasProperty("kotlin.test.android.teamcity")) {
            systemProperty("kotlin.test.android.teamcity", true)
        }

        project.findProperty("kotlin.test.android.path.filter")?.let {
            systemProperty("kotlin.test.android.path.filter", it.toString())
        }

        workingDir = rootDir
        androidSdkProvisioner {
            provideToThisTaskAsSystemProperty(ProvisioningType.SDK_WITH_EMULATOR)
        }
        extensions.configure<TestInputsCheckExtension>("testInputsCheck") {
            // Android emulator setup writes AVD metadata under the provisioned SDK directory.
            val androidSdkRoot = rootDir.resolve("dependencies/android-sdk/build/androidSdk")
            extraPermissions.add("""permission java.io.FilePermission "${androidSdkRoot.absolutePath}", "read";""")
            extraPermissions.add("""permission java.io.FilePermission "${androidSdkRoot.absolutePath}/-", "read,write,delete";""")
            // Android tests execute temporary gradle wrappers/scripts in java.io.tmpdir.
            val tmpDir = File(System.getProperty("java.io.tmpdir"))
            extraPermissions.add("""permission java.io.FilePermission "${tmpDir.absolutePath}/-", "execute";""")
            // Emulator process management runs shell utilities directly.
            val executablePaths = listOf(
                "/bin/sh", "/usr/bin/sh",
                "/bin/ps", "/usr/bin/ps",
                "/bin/grep", "/usr/bin/grep",
                "/bin/kill", "/usr/bin/kill",
                "/bin/pidof", "/usr/bin/pidof",
                "/bin/pgrep", "/usr/bin/pgrep",
                "${androidSdkRoot.absolutePath}/platform-tools/adb",
                "${androidSdkRoot.absolutePath}/emulator/emulator",
                "${androidSdkRoot.absolutePath}/cmdline-tools/bin/avdmanager",
            )
            for (path in executablePaths) {
                extraPermissions.add("""permission java.io.FilePermission "$path", "read,execute";""")
            }
        }
    }

    withJvmStdlibAndReflect()
}

val generateAndroidTests by generator(
    "org.jetbrains.kotlin.android.tests.CodegenTestsOnAndroidGenerator",
    testSourceSet,
    inputKind = GeneratorInputKind.RuntimeClasspath,
) {
    dependsOn(rootProject.tasks.named("dist"))
}
