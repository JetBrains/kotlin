// WITH_STDLIB
package usage

class MyClass1
open class ABC {
    open val nestedBlocks = ArrayList<MyClass1>()

    fun makeInjectionBlocks() {
        val l: List<Any> = listOf(1)
        for (b in l) {
            when (b) {
                is Int -> <!TYPE_MISMATCH!><!VAL_REASSIGNMENT!>nestedBlocks<!> += b<!>
                else -> {}
            }
        }
    }
}
