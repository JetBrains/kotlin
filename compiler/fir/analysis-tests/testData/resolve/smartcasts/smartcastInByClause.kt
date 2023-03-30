// ISSUE: KT-49747
// DUMP_CFG

class A(val path: String?, val index: Int)

interface Base
class Derived(val index: Int) : Base

fun test(a: A?): Base? {
    val path = a?.path ?: return null
    takeInt(a.index) // should be ok
    return object : Base by Derived(a.index) {
        val x: Int = a.index

        fun foo() {
            takeInt(a.index)
        }
    }
}

fun takeInt(x: Int) {}
