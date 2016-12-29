class Foo(val a: String)

fun test(s: (Foo) -> String): String {
    return s(Foo("OK"))
}

fun box(): String {
    return test(Foo::a)
}

// method: PropertyReferenceGetKt$box$1::get
// jvm signature:     (Ljava/lang/Object;)Ljava/lang/Object;
// generic signature: null
