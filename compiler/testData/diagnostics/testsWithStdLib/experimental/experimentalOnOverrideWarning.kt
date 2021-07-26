// !USE_EXPERIMENTAL: kotlin.RequiresOptIn
// !LANGUAGE: -OptInOnOverrideForbidden

@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
@Retention(AnnotationRetention.BINARY)
annotation class E

class My {
    <!EXPERIMENTAL_ANNOTATION_ON_OVERRIDE_WARNING!>@E<!>
    override fun hashCode() = 0
}

