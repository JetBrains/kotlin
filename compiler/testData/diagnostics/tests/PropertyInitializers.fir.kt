// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

class Foo(val a: Int, b: Int) {
    val c = a + b

    val d: Int
        get() = a

    val e: Int
        get() = <!UNRESOLVED_REFERENCE!>b<!>

    val map: Map<String, Int> = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH("String; Int"), TYPE_MISMATCH("String; Int"), TYPE_MISMATCH("Int; String")!>mapOf(1 to "hello")<!>
}
