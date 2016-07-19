// See KT-7290
class MyClass(val x: String?)
fun foo(y: MyClass?): Int {
    // x here is smartcast but y is not
    val z = y?.x?.subSequence(0, <!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>y<!>.x<!>.length)
    // !! is necessary here
    y!!.x
    return z?.length ?: -1
}
fun bar(y: MyClass?) {
    y?.x!!.length
    // !! is NOT necessary here, because y?.x != null
    y<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>.x
}