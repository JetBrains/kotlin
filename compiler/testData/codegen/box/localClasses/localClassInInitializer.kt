// JVM_ABI_K1_K2_DIFF: KT-63901

class A {
    var a: String = "Fail"

    init {
        open class B() {
            open fun s() : String = "O"
        }

        val o = object : B() {
            override fun s(): String = "K"
        }

        a = B().s() + o.s()
    }
}

fun box() : String {
    return A().a
}