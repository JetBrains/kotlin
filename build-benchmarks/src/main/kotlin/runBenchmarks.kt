import org.jetbrains.kotlin.build.benchmarks.*

fun main() {
    // expected working dir is %KOTLIN_PROJECT_PATH%/build-benchmarks/
    mainImpl(artifactTransformBenchmarks(), "../.")
}
