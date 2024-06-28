class A<in T, out K>
class B

fun test() {
    val a1 = A<<!PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT!>in<!> Int, <!PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT!>out<!> B>()
    val a2 = A<Int, B>()
    val a3 = A<<!PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT!>*<!>, <!PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT!>*<!>>()
}
