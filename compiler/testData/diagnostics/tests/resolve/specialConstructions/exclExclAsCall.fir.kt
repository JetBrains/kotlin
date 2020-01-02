// !WITH_NEW_INFERENCE
package a

interface A

fun <T>id(t: T): T = t
fun doList(l: List<Int>) = l
fun doInt(i: Int) = i

fun <T> strangeNullableList(f: (T) -> Unit): List<T>? = throw Exception()
fun <T: A> emptyNullableListOfA(): List<T>? = null

//-------------------------------

fun testExclExcl() {
    doList(emptyNullableListOfA()!!) //should be an error here
    val l: List<Int> = id(emptyNullableListOfA()!!)

    doList(strangeNullableList { doInt(it) }!!) //lambda should be analyzed (at completion phase)
}

fun testDataFlowInfoAfterExclExcl(a: Int?) {
    doInt(a!!)
    a + 1
}

fun testUnnecessaryExclExcl(a: Int) {
    doInt(a!!) //should be warning
}