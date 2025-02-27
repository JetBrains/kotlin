import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.kotlin.arguments.CompilerArguments
import org.jetbrains.kotlin.arguments.KotlinArgumentValueType
import org.jetbrains.kotlin.arguments.KotlinReleaseVersions
import org.jetbrains.kotlin.arguments.asReleaseDependent
import org.jetbrains.kotlin.arguments.compilerArgumentsLevel
import java.io.File

val deprecatedCommonArgs by compilerArgumentsLevel("commonCompilerArguments") {
    compilerArgument {
        name = "another-test"
        description = "TBA"

        valueType = KotlinArgumentValueType.BooleanType()
        valueDescription = "true|false"

        addedInVersion = KotlinReleaseVersions.v1_4_0
        deprecatedInVersion = KotlinReleaseVersions.v1_9_20
        removedInVersion = KotlinReleaseVersions.v2_0_0
    }

    subLevel("jvmCompilerArguments") {
        compilerArgument {
            name = "old-option"
            description = "TBA"

            valueType = KotlinArgumentValueType.BooleanType()
            valueDescription = "true|false"

            addedInVersion = KotlinReleaseVersions.v1_4_0
            deprecatedInVersion = KotlinReleaseVersions.v1_9_20
            removedInVersion = KotlinReleaseVersions.v2_0_0
        }
    }
}

val kotlinCompilerArguments = compilerArguments {
    topLevel("commonToolArguments") {
        compilerArgument {
            name = "help"
            shortName = "h"
            description = "Print a synopsis of standard options."

            valueType = KotlinArgumentValueType.BooleanType(
                isNullable = false.asReleaseDependent(),
                defaultValue = false.asReleaseDependent()
            )

            addedInVersion = KotlinReleaseVersions.v1_4_0
            stableSinceVersion = KotlinReleaseVersions.v1_4_0
        }

        compilerArgument {
            name = "X"
            description = "Print a synopsis of advanced options."

            valueType = KotlinArgumentValueType.BooleanType(
                isNullable = false.asReleaseDependent(),
                defaultValue = false.asReleaseDependent()
            )

            addedInVersion = KotlinReleaseVersions.v1_9_20
        }

        compilerArgument {
            name = "version"
            description = "Display the compiler version."

            valueType = KotlinArgumentValueType.BooleanType(
                isNullable = false.asReleaseDependent(),
                defaultValue = false.asReleaseDependent()
            )

            addedInVersion = KotlinReleaseVersions.v1_9_20
        }

        compilerArgument {
            name = "verbose"
            description = "Enable verbose logging output."

            valueType = KotlinArgumentValueType.BooleanType(
                isNullable = false.asReleaseDependent(),
                defaultValue = false.asReleaseDependent()
            )

            addedInVersion = KotlinReleaseVersions.v1_9_20
        }

        compilerArgument {
            name = "nowarn"
            description = "Don't generate any warnings."

            valueType = KotlinArgumentValueType.BooleanType(
                isNullable = false.asReleaseDependent(),
                defaultValue = false.asReleaseDependent()
            )

            addedInVersion = KotlinReleaseVersions.v1_9_20
        }

        compilerArgument {
            name = "Werror"
            description = "Report an error if there are any warnings."

            valueType = KotlinArgumentValueType.BooleanType(
                isNullable = false.asReleaseDependent(),
                defaultValue = false.asReleaseDependent()
            )

            addedInVersion = KotlinReleaseVersions.v1_9_20
        }

        subLevel("commonCompilerArguments", mergeWith = setOf(deprecatedCommonArgs)) {

            compilerArgument {
                name = "language-version"
                description = "Provide source compatibility with the specified version of Kotlin."

                valueType = KotlinArgumentValueType.KotlinVersionType()
                valueDescription = "<version>"

                addedInVersion = KotlinReleaseVersions.v1_9_20
            }

            compilerArgument {
                name = "api-version"
                description = "Allow using declarations from only the specified version of bundled libraries."

                valueType = KotlinArgumentValueType.KotlinVersionType()
                valueDescription = "<version>"

                addedInVersion = KotlinReleaseVersions.v1_4_0
            }

            subLevel("jvmCompilerArguments") {
                compilerArgument {
                    name = "jvm-target"
                    description = "Target version of the generated JVM bytecode (1.6 or 1.8)."

                    valueType = KotlinArgumentValueType.KotlinJvmTargetType()
                    valueDescription = "<version>"

                    addedInVersion = KotlinReleaseVersions.v1_4_0
                }
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

    val decodedCompilerArguments = format.decodeFromString<CompilerArguments>(jsonArguments)
    println("Decoded arguments: $decodedCompilerArguments")
}