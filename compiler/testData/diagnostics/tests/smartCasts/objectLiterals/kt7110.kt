//  Anonymous object's initialization does not affect smart casts

abstract class A(val s: String) {
    fun bar(): String = s
}

fun foo(o: String?): Int {
    val a = object : A(o!!){}
    a.bar()
    return <!DEBUG_INFO_SMARTCAST!>o<!>.length
}
