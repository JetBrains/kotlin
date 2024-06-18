// ISSUE: KT-69182

@RequiresOptIn
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class Internal

enum class Foo {
    Bar,
    ;

    @Internal
    companion object
}


@Internal
enum class Baz {
    Bar
}

val FooBar = Foo.<!OPT_IN_USAGE_ERROR!>Bar<!>
val BazBar = <!OPT_IN_USAGE_ERROR!>Baz<!>.<!OPT_IN_USAGE_ERROR!>Bar<!>

val FooEntries = Foo.<!OPT_IN_USAGE_ERROR!>entries<!>
val BazEntries = <!OPT_IN_USAGE_ERROR!>Baz<!>.<!OPT_IN_USAGE_ERROR!>entries<!>
