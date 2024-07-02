// FIR_IDENTICAL
// IGNORE_BACKEND_K1: JS_IR
// REASON: There is no library for descriptor <m1>

// SKIP_KLIB_TEST
// REASON: KT-69587 Multi-module is not deserialized in JS irText

// MODULE: m1
// FILE: BaseFirBuilder.kt

abstract class BaseFirBuilder<T> {
    inline fun <T> withCapturedTypeParameters(block: () -> T): T {
        return block()
    }
}

// MODULE: m2(m1)
// FILE: FirBuilder.kt

open class BaseConverter : BaseFirBuilder<Any>()

class DeclarationsConverter : BaseConverter()
