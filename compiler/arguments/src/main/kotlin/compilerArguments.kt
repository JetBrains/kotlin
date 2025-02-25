import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.kotlin.arguments.CompilerArgumentsTopLevel
import org.jetbrains.kotlin.arguments.KotlinArgumentValueType
import org.jetbrains.kotlin.arguments.KotlinReleaseVersion

val kotlinCompilerArguments = compilerArguments("commonToolArguments") {
    compilerArgument {
        name = "help"
        shortName = "h"
        description = "Print a synopsis of standard options."

        valueType = KotlinArgumentValueType.BooleanType(
            isNullable = false,
            defaultValue = false
        )

        addedInVersion = KotlinReleaseVersion.KOTLIN_1_4_0
        stableSinceVersion = KotlinReleaseVersion.KOTLIN_1_4_0
    }

    compilerArgument {
        name = "X"
        description = "Print a synopsis of advanced options."

        valueType = KotlinArgumentValueType.BooleanType(
            isNullable = false,
            defaultValue = false
        )

        addedInVersion = KotlinReleaseVersion.KOTLIN_1_9_20
    }

    compilerArgument {
        name = "version"
        description = "Display the compiler version."

        valueType = KotlinArgumentValueType.BooleanType(
            isNullable = false,
            defaultValue = false
        )

        addedInVersion = KotlinReleaseVersion.KOTLIN_1_9_20
    }

    compilerArgument {
        name = "verbose"
        description = "Enable verbose logging output."

        valueType = KotlinArgumentValueType.BooleanType(
            isNullable = false,
            defaultValue = false
        )

        addedInVersion = KotlinReleaseVersion.KOTLIN_1_9_20
    }

    compilerArgument {
        name = "nowarn"
        description = "Don't generate any warnings."

        valueType = KotlinArgumentValueType.BooleanType(
            isNullable = false,
            defaultValue = false
        )

        addedInVersion = KotlinReleaseVersion.KOTLIN_1_9_20
    }

    compilerArgument {
        name = "Werror"
        description = "Report an error if there are any warnings."

        valueType = KotlinArgumentValueType.BooleanType(
            isNullable = false,
            defaultValue = false
        )

        addedInVersion = KotlinReleaseVersion.KOTLIN_1_9_20
    }

    subLevel("commonCompilerArguments") {

        compilerArgument {
            name = "language-version"
            description = "Provide source compatibility with the specified version of Kotlin."

            valueType = KotlinArgumentValueType.KotlinVersionType()
            valueDescription = "<version>"

            addedInVersion = KotlinReleaseVersion.KOTLIN_1_4_0
        }

        compilerArgument {
            name = "api-version"
            description = "Allow using declarations from only the specified version of bundled libraries."

            valueType = KotlinArgumentValueType.KotlinVersionType()
            valueDescription = "<version>"

            addedInVersion = KotlinReleaseVersion.KOTLIN_1_4_0
        }

        subLevel("jvmCompilerArguments") {
            compilerArgument {
                name = "jvm-target"
                description = "Target version of the generated JVM bytecode (1.6 or 1.8)."

                valueType = KotlinArgumentValueType.KotlinJvmTargetType()
                valueDescription = "<version>"

                addedInVersion = KotlinReleaseVersion.KOTLIN_1_4_0
            }
        }
    }
}

fun main() {
    val format = Json {
        prettyPrint = true
        classDiscriminator = "#class"
        encodeDefaults = true
    }

    val jsonArguments = format.encodeToString(kotlinCompilerArguments)
    println("=== arguments in JSON ===")
    println(jsonArguments)
    println("=== end of JSON ===")
    val jsonFile = File("./compiler/arguments/arguments.json")
    jsonFile.writeText(jsonArguments)

    val decodedCompilerArguments = format.decodeFromString<CompilerArgumentsTopLevel>(jsonArguments)
    println("Decoded arguments: $decodedCompilerArguments")
}