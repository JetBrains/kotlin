// !DIAGNOSTICS: -UNUSED_PARAMETER

interface Test1 {
    val test1: Int
}
val Test1.<!EXTENSION_SHADOWED_BY_MEMBER!>test1<!>: Int get() = 42

interface Test2 {
    var test2: Int
}
val Test2.<!EXTENSION_SHADOWED_BY_MEMBER!>test2<!>: Int get() = 42

interface Test3 {
    val test3: Int
}
var Test3.<!EXTENSION_SHADOWED_BY_MEMBER!>test3<!>: Int get() = 42; set(v) {}

interface Test4 {
    val test4: Int
}
var Test4.<!EXTENSION_SHADOWED_BY_MEMBER!>test4<!>: Int get() = 42; set(v) {}

