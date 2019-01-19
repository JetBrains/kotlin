// KJS_WITH_FULL_RUNTIME
package foo

fun box() : String {
    val a = ArrayList<Int>();
    a.add(1)
    a.add(2)
    return if((a.size == 2) && (a.get(1) == 2) && (a.get(0) == 1)) "OK" else "fail"
}
