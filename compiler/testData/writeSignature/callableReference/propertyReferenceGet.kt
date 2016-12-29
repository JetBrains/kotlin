class Foo(val a: String)

fun test(s: () -> String): String {
    return s()
}

fun box(): String {
    return test(Foo("OK")::a)
}

// method: PropertyReferenceGetKt$box$1::get
// jvm signature:     ()Ljava/lang/Object;
// generic signature: null
