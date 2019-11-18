// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

package regressions

class Kt1619Test {

    fun doSomething(list: List<String?>): Int {
        return list.size
    }

    fun testCollectionNotNullCanBeUsedForNullables(): Int {
        val list: List<String> = arrayListOf("foo", "bar")
        return doSomething(list)
    }
}

fun box(): String {
   return if (Kt1619Test().testCollectionNotNullCanBeUsedForNullables() == 2) "OK" else "fail"
}
