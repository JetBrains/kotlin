fun foo(i: @a Int?) {}

fun foo(l: List<@a Int?>) {}

fun @a Int?.bar() {}

val baz: @a Int?


fun foo1(i: @b(1) Int?) {}

fun foo1(l: List<@b(1) Int?>) {}

fun @b(1) Int?.bar1() {}

val baz1: @b(1) Int?


fun foo2(i: @[a b(1)] Int?) {}

fun foo2(l: List<@[a b(1)] Int?>) {}

fun @[a b(1)] Int?.bar2() {}

val baz2: @[a b(1)] Int?