import org.jetbrains.kotlin.build.benchmarks.*

fun main() {
    mainImpl(kotlinBenchmarks(), "../.") // expected working dir is %KOTLIN_PROJECT_PATH%/build-benchmarks/
}
