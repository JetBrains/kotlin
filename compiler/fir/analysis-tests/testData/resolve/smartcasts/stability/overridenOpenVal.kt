// IGNORE_REVERSED_RESOLVE
// !DUMP_CFG
open class A(open val x: Any)

class B(x: Any) : A(x) {
    fun test_1() {
        if (x is String) {
            x.length
        }
    }
}

fun test_2(b: B) {
    if (b.x is String) {
        b.x.length
    }
}
