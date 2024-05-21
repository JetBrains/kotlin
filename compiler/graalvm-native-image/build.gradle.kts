plugins {
    id("org.graalvm.buildtools.native") version "0.10.1"
    kotlin("jvm")
}

kotlin {
    jvmToolchain(21)
}

graalvmNative {
    toolchainDetection = true

    agent {
        defaultMode = "standard"
    }

    binaries {
        val main by getting {
            imageName = "kotlinc-jvm"
            mainClass = "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler"

            buildArgs(
                "-H:+UnlockExperimentalVMOptions",
                "-H:+AllowJRTFileSystem",
                "-H:+AddAllCharsets",
                "--strict-image-heap",
                "-march=native"
            )

            javaLauncher.set(javaToolchains.launcherFor {
                languageVersion.set(JavaLanguageVersion.of(21))
                vendor.set(JvmVendorSpec.matching("Oracle Corporation"))
            })
        }
    }
}

afterEvaluate {
    dependencies {
        "nativeImageClasspath".invoke(project(":kotlin-compiler"))
    }
}
