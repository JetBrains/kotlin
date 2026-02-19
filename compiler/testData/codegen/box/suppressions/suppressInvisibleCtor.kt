// ISSUE: KT-58421
// TARGET_BACKEND: JVM
// IGNORE_NATIVE: cacheMode=STATIC_USE_HEADERS_EVERYWHERE

// MODULE: lib
// FILE: ContinuationImpl.kt

internal abstract class ContinuationImpl() {
    constructor(arg: Int) : this()
}

// MODULE: main(lib)
// DISABLE_IR_VISIBILITY_CHECKS: ANY
// FILE: box.kt

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE", "NONE_APPLICABLE")
internal class SafeCollector: ContinuationImpl()

fun box() = "OK"