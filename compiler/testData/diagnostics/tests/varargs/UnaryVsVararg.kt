// FIR_IDENTICAL
fun foo1(a : Int) : String = "noarg"

fun foo1(a : Int, vararg t : Int) : String = "vararg"

fun test1() {
    foo1(1)
    val a = IntArray(0)
    foo1(1, *a)
}
