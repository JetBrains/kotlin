plugins {
    kotlin("multiplatform") version "@@MPP_GRADLE_PLUGIN_VERSION@@"
}

repositories {
    mavenCentral()@@MPP_CUSTOM_PLUGIN_REPOS_4S@@
}

kotlin {
    // For ARM, preset function should be changed to iosArm32() or iosArm64()
    // For Linux, preset function should be changed to e.g. linuxX64()
    // For MacOS, preset function should be changed to e.g. macosX64()
    @@MPP_HOST_PLATFORM@@("HelloWorld") {
        binaries {
            // Comment the next section to generate Kotlin/Native library (KLIB) instead of executable file:
            executable("HelloWorldApp") {
                // Change to specify fully qualified name of your application's entry point:
                entryPoint = "sample.helloworld.main"
            }
        }
    }
}

// Use the following Gradle tasks to run your application:
// :runHelloWorldAppReleaseExecutableHelloWorld - without debug symbols
// :runHelloWorldAppDebugExecutableHelloWorld - with debug symbols
