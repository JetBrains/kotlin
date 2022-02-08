// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 1 java/lang/invoke/LambdaMetafactory

fun interface IFoo<T> {
    fun foo(): T
}

fun <T> foo(iFoo: IFoo<T>) = iFoo.foo()

var ok = "Failed"

fun box(): String {
    foo { ok = "OK" }
    return ok
}
