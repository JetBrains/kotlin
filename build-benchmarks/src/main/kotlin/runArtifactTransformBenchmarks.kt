import org.jetbrains.kotlin.build.benchmarks.*

fun main() {
    mainImpl(kotlinBenchmarks(arrayOf("-Dkotlin.incremental.useClasspathSnapshot=true")), "../.") // expected working dir is %KOTLIN_PROJECT_PATH%/build-benchmarks/
}