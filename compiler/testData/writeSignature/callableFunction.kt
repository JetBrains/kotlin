class Foo() {
    fun test(): String = "OK"
}

fun test(s: (Foo) -> String): String {
    return s(Foo())
}

fun box(): String {
    return test(Foo::test)
}

// method: CallableFunctionKt$box$1::invoke
// jvm signature:     (LFoo;)Ljava/lang/String;
// generic signature: null
