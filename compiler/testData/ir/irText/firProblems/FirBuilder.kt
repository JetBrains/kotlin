// IGNORE_BACKEND_K1: JS_IR
// IGNORE_BACKEND_K1: JS_IR_ES6

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
