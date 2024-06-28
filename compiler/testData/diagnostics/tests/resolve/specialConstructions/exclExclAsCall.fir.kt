package a

interface A

fun <T>id(t: T): T = t
fun doList(l: List<Int>) = l
fun doInt(i: Int) = i

fun <T> strangeNullableList(f: (T) -> Unit): List<T>? = throw Exception()
fun <T: A> emptyNullableListOfA(): List<T>? = null

//-------------------------------

fun testExclExcl() {
    <!INFERRED_TYPE_VARIABLE_INTO_POSSIBLE_EMPTY_INTERSECTION("T; a.A, kotlin.Int; final class and interface")!>doList<!>(emptyNullableListOfA()!!) //should be an error here
    val l: List<Int> = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.List<a.A & kotlin.Int>")!>id(<!INFERRED_TYPE_VARIABLE_INTO_POSSIBLE_EMPTY_INTERSECTION!>emptyNullableListOfA<!>()!!)<!>

    doList(strangeNullableList { doInt(it) }!!) //lambda should be analyzed (at completion phase)
}

fun testDataFlowInfoAfterExclExcl(a: Int?) {
    doInt(a!!)
    a + 1
}

fun testUnnecessaryExclExcl(a: Int) {
    doInt(a<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>) //should be warning
}
