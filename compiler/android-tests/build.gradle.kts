import org.jetbrains.kotlin.build.androidsdkprovisioner.ProvisioningType

plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("android-sdk-provisioner")
    id("project-tests-convention")
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

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit4) {
        dependsOn(":dist")
        val jdkHome = project.getToolchainJdkHomeFor(JdkMajorVersion.JDK_1_8)
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
    }

    withJvmStdlibAndReflect()
}
