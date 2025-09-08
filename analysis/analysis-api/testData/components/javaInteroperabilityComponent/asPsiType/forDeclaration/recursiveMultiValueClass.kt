package pack

@JvmInline
value class Foo1(val a: Foo2, val b: Foo1)

@JvmInline
value class Foo2(val a: Foo3, val b: Foo2)

@JvmInline
value class Foo3(val a: Foo1, val b: Foo3)

fun f<caret>oo(): Foo1 {}