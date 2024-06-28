// FULL_JDK
// FIR_IDENTICAL
// WITH_STDLIB

class A<T> : ArrayList<T>() {
    override fun sort(t: Comparator<in T>) {
        super.sort(t)
    }
}

fun foo(x: MutableList<String>, y: ArrayList<String>, z: A<String>, c: Comparator<String>) {
    x.<!DEBUG_INFO_CALL("fqName: kotlin.collections.sort; typeCall: inline extension function")!><!DEPRECATION_ERROR!>sort<!>(c)<!>
    y.<!DEBUG_INFO_CALL("fqName: kotlin.collections.sort; typeCall: inline extension function")!><!DEPRECATION_ERROR!>sort<!>(c)<!>
    z.<!DEBUG_INFO_CALL("fqName: kotlin.collections.sort; typeCall: inline extension function")!><!DEPRECATION_ERROR!>sort<!>(c)<!>
}
