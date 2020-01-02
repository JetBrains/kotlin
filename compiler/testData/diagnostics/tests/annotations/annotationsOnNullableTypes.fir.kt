//!DIAGNOSTICS: -UNUSED_PARAMETER

@Target(AnnotationTarget.TYPE)
annotation class a

@Target(AnnotationTarget.TYPE)
annotation class b(val i: Int)

annotation class c

fun foo(i: @a Int?) {}

fun foo(l: List<@a Int?>) {}

fun @a Int?.bar() {}

val baz: @a Int? = 1


fun foo1(i: @b(1) Int?) {}

fun foo1(l: List<@b(1) Int?>) {}

fun @b(1) Int?.bar1() {}

val baz1: @b(1) Int? = 1


fun foo2(i: @[a b(1)] Int?) {}

fun foo2(l: List<@[a b(1)] Int?>) {}

fun @[a b(1)] Int?.bar2() {}

val baz2: @[a b(1)] Int? = 1


fun foo3(i: @c Int?) {}

fun foo3(l: List<@c Int?>) {}

fun @c Int?.bar3() {}

val baz3: @c Int? = 1