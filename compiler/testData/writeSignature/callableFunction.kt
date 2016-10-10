class Foo() {
    fun test(): String = "OK"
}

fun test(s: () -> String): String {
    return s()
}

fun box(): String {
    return test(Foo()::test)
}

// method: CallableFunctionKt$box$1::invoke
// jvm signature:     ()Ljava/lang/String;
// generic signature: null
