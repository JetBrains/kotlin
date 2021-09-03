// !OPT_IN: kotlin.RequiresOptIn
// !LANGUAGE: -OptInOnOverrideForbidden

@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
@Retention(AnnotationRetention.BINARY)
annotation class E

class My {
    <!OPT_IN_MARKER_ON_OVERRIDE_WARNING!>@E<!>
    override fun hashCode() = 0
}

