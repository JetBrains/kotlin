// LANGUAGE: -ReportTypeVarianceConflictOnQualifierArguments

class Bar<K> {
    inner class Inner {
        inner class SuperInner
    }
}

abstract class Foo<in T> {
    abstract fun yuckyEventHandler(
        fn: Bar<<!TYPE_VARIANCE_CONFLICT_WARNING!>T<!>>.Inner.() -> Unit
    ): () -> Unit

    abstract fun second(fn: Bar<<!TYPE_VARIANCE_CONFLICT_WARNING!>T<!>>.Inner)

    abstract fun third(fn: Bar<<!TYPE_VARIANCE_CONFLICT_WARNING!>T<!>>.Inner.SuperInner)
}

abstract class Baz<out T> {
    abstract fun yuckyEventHandler(
        fn: Bar<<!TYPE_VARIANCE_CONFLICT_WARNING!>T<!>>.Inner.() -> Unit
    ): () -> Unit
}
