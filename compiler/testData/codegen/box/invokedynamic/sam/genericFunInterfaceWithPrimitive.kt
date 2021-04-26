// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 2 java/lang/invoke/LambdaMetafactory

fun interface IFoo<T> {
    fun foo(x: T): T
}

fun interface IBar<T : Any> {
    fun bar(x: T): T
}


fun foo1(foo: IFoo<Int>) = foo.foo(1)

fun bar1(bar: IBar<Int>) = bar.bar(1)

fun box(): String {
    val t = foo1 { it + 41 }
    if (t != 42) return "Failed: t=$t"

    val tt = bar1 { it + 41 }
    if (tt != 42) return "Failed: tt=$tt"

    return "OK"
}