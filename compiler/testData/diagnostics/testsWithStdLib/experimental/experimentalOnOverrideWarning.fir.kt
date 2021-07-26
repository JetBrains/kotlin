// !USE_EXPERIMENTAL: kotlin.RequiresOptIn
// !LANGUAGE: -OptInOnOverrideForbidden

@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
@Retention(AnnotationRetention.BINARY)
annotation class E

class My {
    @E
    override fun hashCode() = 0
}

