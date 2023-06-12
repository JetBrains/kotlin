// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 0 java/lang/invoke/LambdaMetafactory
// 1 class FunInterfaceWithInlineNStringKt\$box\$t\$1

inline class Z(val value: String?)

fun interface IFooZ {
    fun foo(x: Z): Z
}

fun foo1(fs: IFooZ) = fs.foo(Z("O"))

fun box(): String {
    val t = foo1 { Z(it.value!! + "K") }
    if (t.value != "OK") return "Failed: t=$t"

    return "OK"
}
