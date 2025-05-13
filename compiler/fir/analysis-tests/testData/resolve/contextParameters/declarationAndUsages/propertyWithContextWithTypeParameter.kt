// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// LANGUAGE: +ContextParameters

context(b: T)
val <T> property1: String
    get() = ""

context(b: T&Any)
val <T: String?> property2: String
    get() = ""

context(a: Map<T1, T2>)
val <T1, T2> property3: String
    get() = ""

context(a: Map<String, <!REDUNDANT_PROJECTION!>out<!> T>)
val <T> property4: String
    get() = ""

context(a:  Map<String, <!CONFLICTING_PROJECTION!>in<!> T>)
val <T> property5: String
    get() = ""

context(a: T)
val <T: K, K> property6: String
    get() = ""

fun usageContextProperty() {
    with("") {
        property1
        property2
        property6
    }
    with(mapOf("" to "")){
        property3
        property4
    }
}