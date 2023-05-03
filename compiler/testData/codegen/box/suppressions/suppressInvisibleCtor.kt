// ISSUE: KT-58421
// TARGET_BACKEND: JVM

// MODULE: lib
// FILE: ContinuationImpl.kt

internal abstract class ContinuationImpl() {
    constructor(arg: Int) : this()
}

// MODULE: main(lib)
// FILE: box.kt

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE", "NONE_APPLICABLE")
internal class SafeCollector: ContinuationImpl()

fun box() = "OK"