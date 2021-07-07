// FIR_IDENTICAL
// !USE_EXPERIMENTAL: kotlin.RequiresOptIn

@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
annotation class E1

class My(@E1 val x: Int)
