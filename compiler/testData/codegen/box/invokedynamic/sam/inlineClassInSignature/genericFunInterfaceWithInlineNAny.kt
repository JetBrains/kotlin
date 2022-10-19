// WITH_STDLIB
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 1 java/lang/invoke/LambdaMetafactory

inline class Z(val value: Any?)

fun interface IFoo<T> {
    fun foo(x: T): T
}

fun foo1(fs: IFoo<Z>) = fs.foo(Z(1))

fun box(): String {
    val t = foo1 { Z((it.value as Int) + 41) }
    if (t.value != 42) return "Failed: t=$t"

    return "OK"
}