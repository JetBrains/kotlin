// TARGET_BACKEND: JVM
// MODULE: lib
// WITH_RUNTIME
// FILE: A.kt

abstract class IncrementalCompilerRunner<T>(
    private val workingDir: String,
    val fail: Boolean,
    val output: Collection<String> = emptyList()
) {
    fun res(res: T? = null): String = (res as? String) ?: (if (fail) "FAIL" else workingDir)
}

class IncrementalJsCompilerRunner(
    private val workingDir: String,
    fail: Boolean = true
) : IncrementalCompilerRunner<String>(workingDir, fail) {
}

// MODULE: main(lib)
// FILE: B.kt

fun box(): String {
    val runner = IncrementalJsCompilerRunner(workingDir = "OK", fail = false)
    return runner.res()
}
