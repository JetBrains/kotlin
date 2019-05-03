// !WITH_NEW_INFERENCE
package a

interface A
fun doList(l: List<Int>) = l
fun doInt(i: Int) = i
fun getList(): List<Int>? = null
fun <T> strangeList(f: (T) -> Unit): List<T> = throw Exception("$f")
fun <T: A> emptyListOfA(): List<T> = throw Exception()

//-------------------------------

fun testElvis(a: Int?, b: Int?) {
    if (a != null) {
        doInt(b ?: <!DEBUG_INFO_SMARTCAST!>a<!>)
    }
    doList(getList() ?: <!OI;TYPE_INFERENCE_UPPER_BOUND_VIOLATED!>emptyListOfA<!>()) //should be an error
    doList(getList() ?: strangeList { doInt(it) }) //lambda was not analyzed
}


fun testDataFlowInfo1(a: Int?, b: Int?) {
    val c: Int = a ?: b!!
    doInt(c)
    // b is nullable if a != null
    b <!UNSAFE_OPERATOR_CALL!>+<!> 1
}

fun testDataFlowInfo2(a: Int?, b: Int?) {
    doInt(a ?: b!!)
    // b is nullable if a != null
    b <!UNSAFE_OPERATOR_CALL!>+<!> 1
}

fun testTypeMismatch(a: String?, b: Any) {
    doInt(<!NI;TYPE_MISMATCH!><!OI;TYPE_MISMATCH!>a<!> ?: <!OI;TYPE_MISMATCH!>b<!><!>)
}