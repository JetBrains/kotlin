import org.jetbrains.kotlin.build.benchmarks.*

fun main() {
    mainImpl(abiSnapshotBenchmarks(), "../.") // expected working dir is %KOTLIN_PROJECT_PATH%/build-benchmarks/
}