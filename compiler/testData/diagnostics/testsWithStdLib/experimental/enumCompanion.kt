// FIR_IDENTICAL
// ISSUE: KT-69182

@RequiresOptIn
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
annotation class Internal

enum class Foo {
    Bar,
    @Internal
    Deprecated,
    ;

    @Internal
    companion object
}

@Internal
enum class Baz {
    Bar
}

val FooBar = Foo.Bar
val BazBar = <!OPT_IN_USAGE_ERROR!>Baz<!>.<!OPT_IN_USAGE_ERROR!>Bar<!>

val FooEntries = Foo.entries
val BazEntries = <!OPT_IN_USAGE_ERROR!>Baz<!>.<!OPT_IN_USAGE_ERROR!>entries<!>

val FooDeprecated = Foo.<!OPT_IN_USAGE_ERROR!>Deprecated<!>
