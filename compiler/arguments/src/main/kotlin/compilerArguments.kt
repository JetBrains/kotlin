import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.kotlin.arguments.types.BooleanType
import org.jetbrains.kotlin.arguments.CompilerArguments
import org.jetbrains.kotlin.arguments.types.KotlinJvmTargetType
import org.jetbrains.kotlin.arguments.types.KotlinVersionType
import org.jetbrains.kotlin.arguments.KotlinReleaseVersions
import org.jetbrains.kotlin.arguments.ReleaseDependent
import org.jetbrains.kotlin.arguments.asReleaseDependent
import org.jetbrains.kotlin.arguments.compilerArgument
import org.jetbrains.kotlin.arguments.compilerArgumentsLevel
import java.io.File

val someArguments by compilerArgument {
    name = "some-argument"
    description = ReleaseDependent(
        "The awesome argument to make compilation fast",
        KotlinReleaseVersions.v1_4_0..KotlinReleaseVersions.v1_9_20 to "Slows your compilation"
    )

    valueType = BooleanType()
    valueDescription = "true|false".asReleaseDependent()

    lifecycle(
        introducedVersion = KotlinReleaseVersions.v1_4_0,
        stabilizedVersion = KotlinReleaseVersions.v1_4_0,
    )
}

val deprecatedCommonArgs by compilerArgumentsLevel("commonCompilerArguments") {

    addCompilerArguments(someArguments)

    compilerArgument {
        name = "another-test"
        description = "TBA".asReleaseDependent()

        valueType = BooleanType()
        valueDescription = "true|false".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersions.v1_4_0,
            stabilizedVersion = KotlinReleaseVersions.v1_4_0,
            deprecatedVersion = KotlinReleaseVersions.v1_9_20,
            removedVersion = KotlinReleaseVersions.v2_0_0,
        )
    }

    subLevel("jvmCompilerArguments") {
        compilerArgument {
            name = "old-option"
            description = "TBA".asReleaseDependent()

            valueType = BooleanType()
            valueDescription = "true|false".asReleaseDependent()

            lifecycle(
                introducedVersion = KotlinReleaseVersions.v1_4_0,
                deprecatedVersion = KotlinReleaseVersions.v1_9_20,
                removedVersion = KotlinReleaseVersions.v2_0_0,
            )
        }
    }
}

val kotlinCompilerArguments = compilerArguments {
    topLevel("commonToolArguments") {
        compilerArgument {
            name = "help"
            shortName = "h"
            description = "Print a synopsis of standard options.".asReleaseDependent()

            valueType = BooleanType(
                isNullable = false.asReleaseDependent(),
                defaultValue = false.asReleaseDependent()
            )

            lifecycle(
                introducedVersion = KotlinReleaseVersions.v1_4_0,
                stabilizedVersion = KotlinReleaseVersions.v1_4_0
            )
        }

        compilerArgument {
            name = "X"
            description = "Print a synopsis of advanced options.".asReleaseDependent()

            valueType = BooleanType(
                isNullable = false.asReleaseDependent(),
                defaultValue = false.asReleaseDependent()
            )

            lifecycle(
                introducedVersion = KotlinReleaseVersions.v1_9_20
            )
        }

        compilerArgument {
            name = "version"
            description = "Display the compiler version.".asReleaseDependent()

            valueType = BooleanType(
                isNullable = false.asReleaseDependent(),
                defaultValue = false.asReleaseDependent()
            )

            lifecycle(
                introducedVersion = KotlinReleaseVersions.v1_9_20
            )
        }

        compilerArgument {
            name = "verbose"
            description = "Enable verbose logging output.".asReleaseDependent()

            valueType = BooleanType(
                isNullable = false.asReleaseDependent(),
                defaultValue = false.asReleaseDependent()
            )

            lifecycle(
                introducedVersion = KotlinReleaseVersions.v1_9_20
            )
        }

        compilerArgument {
            name = "nowarn"
            description = "Don't generate any warnings.".asReleaseDependent()

            valueType = BooleanType(
                isNullable = false.asReleaseDependent(),
                defaultValue = false.asReleaseDependent()
            )

            lifecycle(
                introducedVersion = KotlinReleaseVersions.v1_9_20
            )
        }

        compilerArgument {
            name = "Werror"
            description = "Report an error if there are any warnings.".asReleaseDependent()

            valueType = BooleanType(
                isNullable = false.asReleaseDependent(),
                defaultValue = false.asReleaseDependent()
            )

            lifecycle(
                introducedVersion = KotlinReleaseVersions.v1_9_20
            )
        }

        subLevel("commonCompilerArguments", mergeWith = setOf(deprecatedCommonArgs)) {

            compilerArgument {
                name = "language-version"
                description = "Provide source compatibility with the specified version of Kotlin.".asReleaseDependent()

                valueType = KotlinVersionType()
                valueDescription = "<version>".asReleaseDependent()

                lifecycle(
                    introducedVersion = KotlinReleaseVersions.v1_9_20
                )
            }

            compilerArgument {
                name = "api-version"
                description = "Allow using declarations from only the specified version of bundled libraries.".asReleaseDependent()

                valueType = KotlinVersionType()
                valueDescription = "<version>".asReleaseDependent()

                lifecycle(
                    introducedVersion = KotlinReleaseVersions.v1_4_0
                )
            }

            subLevel("jvmCompilerArguments") {
                compilerArgument {
                    name = "jvm-target"
                    description = "Target version of the generated JVM bytecode (1.6 or 1.8).".asReleaseDependent()

                    valueType = KotlinJvmTargetType()
                    valueDescription = "<version>".asReleaseDependent()

                    lifecycle(
                        introducedVersion = KotlinReleaseVersions.v1_4_0
                    )
                }
            }
        }
    }
}

fun main() {
    val format = Json {
        prettyPrint = true
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