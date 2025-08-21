import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

val projectsAllowedToUseFirFromSymbol = listOf(
    "analysis-tests",
    "dump",
    "fir-deserialization",
    "fir-serialization",
    "fir2ir",
    "fir-jvm",
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
        tasks.withType<KotlinJvmCompile>().configureEach {
            compilerOptions.optIn.addAll(
                listOf(
                    "org.jetbrains.kotlin.fir.symbols.SymbolInternals",
                    "org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess",
                    "org.jetbrains.kotlin.types.model.K2Only",
                )
            )
        }
    }
}
