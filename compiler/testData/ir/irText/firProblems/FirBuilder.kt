// FIR_IDENTICAL
// IGNORE_BACKEND_K1: NATIVE
// REASON: ClassicFrontendFacade.performNativeModuleResolve expects DependencyKind.Source, not DependencyKind.Binary
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
