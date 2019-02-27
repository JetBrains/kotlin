plugins {
    kotlin("multiplatform") version "$CIDR_PLUGIN_VERSION"
}

repositories {
    mavenCentral()
#foreach($repo in $CIDR_CUSTOM_PLUGIN_REPOS)
    maven("$repo")
#end
}

kotlin {
    // For ARM, preset function should be changed to iosArm32() or iosArm64()
    // For Linux, preset function should be changed to e.g. linuxX64()
    // For MacOS, preset function should be changed to e.g. macosX64()
    $CIDR_MPP_PLATFORM("CSVParser") {
        binaries {
            // Comment the next section to generate Kotlin/Native library (KLIB) instead of executable file:
            executable("CSVParserApp") {
                // Change to specify fully qualified name of your application's entry point:
                entryPoint = "sample.csvparser.main"
                runTask?.args("./European_Mammals_Red_List_Nov_2009.csv", "4", "100")
            }
        }
    }
}

// Use the following Gradle tasks to run your application:
// :runCSVParserAppReleaseExecutableCSVParser - without debug symbols
// :runCSVParserAppDebugExecutableCSVParser - with debug symbols
