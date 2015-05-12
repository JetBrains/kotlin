interface A
interface B : A {
    fun foo()
}

fun bar1(a: A) {
    var b: B = a as B
    <!DEBUG_INFO_SMARTCAST!>a<!>.foo()
    b.foo()
}

fun id(b: B) = b
fun bar2(a: A) {
    var b: B = id(a as B)
    <!DEBUG_INFO_SMARTCAST!>a<!>.foo()
    b.foo()
}
