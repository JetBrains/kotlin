// !DIAGNOSTICS: -UNUSED_VARIABLE -CAST_NEVER_SUCCEEDS -CANNOT_CHECK_FOR_ERASED -UNCHECKED_CAST -UNUSED_ANONYMOUS_PARAMETER
// SKIP_TXT
// Issue: KT-31734

@Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
@Repeatable
annotation class Foo

fun foo1(x: @Foo() () -> Unit) = x as Iterable<@Foo() () -> Unit>?

fun foo2() = null as @Foo() () -> Unit

fun foo3(x: Any?) {
    if (x is (@<!DEBUG_INFO_MISSING_UNRESOLVED!>Foo<!>() () -> Unit)?) {

    }
}

fun foo4(x: Any) = x is @Foo() () -> (() -> Unit?)

fun foo5(x: Any): @Foo() () -> Unit = x as @Foo () @[Foo Foo ()] @Foo() () -> Unit

fun foo6() {
    val x: @Foo() @[Foo Foo()] @Foo() () -> Unit = {}
}

fun foo7() {
    val x: @Foo() (@Foo() () -> Unit) -> Unit = { x: @Foo() () -> Unit -> }
}

fun foo8(x: @[Foo() ] () -> Unit) = x as Iterable<@Foo() () -> Unit>?

fun foo9(x: @[Foo()] () -> Unit) = x as Iterable<@Foo() () -> Unit>?

fun foo10() {
    val x: @Foo () @Foo () () -> Unit = {}
}

fun foo11() {
    val x: @Foo @Foo () () -> Unit = {}
}

fun foo12() {
    val x: @Foo() @Foo () () -> Unit = {}
}
