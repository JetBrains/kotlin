// LANGUAGE: -NonParenthesizedAnnotationsOnFunctionalTypes
// DIAGNOSTICS: -UNUSED_VARIABLE -CAST_NEVER_SUCCEEDS -CANNOT_CHECK_FOR_ERASED -UNCHECKED_CAST -UNUSED_ANONYMOUS_PARAMETER
// SKIP_TXT
// Issue: KT-31734

@Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
@Repeatable
annotation class Foo

fun foo1(x: @Foo () -> Unit) = x as Iterable<@Foo () -> Unit>?

fun foo2() = null as @Foo () -> Unit

fun foo3(x: Any?) {
    if (x is (@Foo () -> Unit)?) {

    }
}

fun foo4(x: Any) = x is @Foo () -> (() -> Unit?)

fun foo5(x: Any): @Foo () -> Unit = x as @Foo() @[Foo Foo()] @Foo () -> Unit

fun foo6() {
    val x: @Foo() @[Foo Foo()] @Foo () -> Unit = {}
}

fun foo7() {
    val x: @Foo (@Foo () -> Unit) -> Unit = { x: @Foo () -> Unit -> }
}

fun foo8(x: Any?) {
    val x: (@Foo () -> Unit)? = {}
}

fun foo9(x: (@Foo () -> Unit)?) = x as Iterable<(@Foo () -> Unit?)?>?

fun foo10(x: @[Foo] () -> Unit) = x as Iterable<@Foo() () -> Unit>?

fun foo11(x: @[Foo ] () -> Unit) = x as Iterable<@Foo() () -> Unit>?

fun foo12(x: @[Foo/**/] () -> Unit) = x as Iterable<@Foo() () -> Unit>?

val foo13: @Foo (x: @Foo Any) -> Unit get() = {}

val foo14: @Foo (x: @Foo () -> Unit) -> Unit get() = {}

val foo15: @Foo () @Foo () -> Unit get() = {}

val foo16: @Foo @Foo () @Foo () -> Unit get() = {}

val foo17: @Foo() @Foo () @Foo () -> Unit get() = {}

val foo18: @Foo()@Foo () @Foo () -> Unit get() = {}

val foo19: @Foo@Foo () @Foo () -> Unit get() = {}

val foo20: @Foo@Foo () -> Unit get() = {}

val foo21: @Foo()@Foo () -> Unit get() = {}

val foo22: @Foo (x: @Foo () -> Unit) -> Unit get() = {}

val foo23: @Foo (@Foo () -> Unit) -> Unit get() = {}

val foo24: @Foo (@Foo () -> Unit, @Foo () -> Unit) -> Unit get() = {x, y -> }

val foo25: @Foo (x: @Foo Any, @Foo Any) -> Unit get() = {x, y -> }

val foo26: @Foo suspend () -> Unit = {}
