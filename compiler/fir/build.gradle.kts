plugins {
    id("org.jetbrains.kotlinx.kover")
}

val projectsAllowedToUseFirFromSymbol = listOf(
    "analysis-tests",
    "dump",
    "fir-deserialization",
    "fir-serialization",
    "fir2ir",
    "java",
    "jvm",
    "raw-fir",
    "providers",
    "semantics",
    "resolve",
    "tree",
    "jvm-backend",
    "light-tree2fir",
    "psi2fir",
    "raw-fir.common"
)

subprojects {
    if (name in projectsAllowedToUseFirFromSymbol) {
        tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>>().configureEach {
            kotlinOptions {
                freeCompilerArgs += "-opt-in=org.jetbrains.kotlin.fir.symbols.SymbolInternals"
            }
        }
    }
}

dependencies {
    kover(project("analysis-tests"))
    kover (project("checkers"))
    kover (project("cones"))
    kover (project("dump"))
    kover (project("entrypoint"))
    kover (project("fir2ir"))
    kover (project("fir-deserialization"))
    kover (project("fir-serialization"))
    kover (project("java"))
    kover (project("plugin-utils"))
    kover (project("providers"))
    kover (project("raw-fir"))
    kover (project("resolve"))
    kover (project("semantics"))
    kover (project("tree"))
}

subprojects {
    apply(plugin = "org.jetbrains.kotlinx.kover")
}

projectTest {
//    ignoreFailures = true
}