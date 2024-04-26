import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    kotlin("multiplatform") version "1.9.23"
    kotlin("native.cocoapods") version "1.9.23"
    id("org.kmp_apple_privacy_manifests.publication") version "+"
}

kotlin {
    val xcf = XCFramework()
    listOf(
        // Thin
        iosArm64(),

        // Universal
        watchosSimulatorArm64(),
        watchosX64(),
    ).forEach {
        it.binaries.framework {
            xcf.add(this)
        }
    }

    cocoapods {
        version = "1.0"
        homepage = "foo"
        summary = "bar"
    }

    privacyManifestConfiguration {
        embedPrivacyManifest(
            layout.projectDirectory.file("PrivacyInfo.xcprivacy").asFile
        )
    }
}