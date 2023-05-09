// WITH_STDLIB

class Foo(val a: Int, b: Int) {
    val c = a + b

    val d: Int
        get() = a

    val e: Int
        get() = <!UNRESOLVED_REFERENCE!>b<!>

    val map: Map<String, Int> = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH("kotlin/String; kotlin/Int"), TYPE_MISMATCH("kotlin/String; kotlin/Int"), TYPE_MISMATCH("kotlin/Int; kotlin/String"), TYPE_MISMATCH("kotlin/String; kotlin/Int"), TYPE_MISMATCH("kotlin/Int; kotlin/String")!>mapOf(1 to "hello")<!>
}
