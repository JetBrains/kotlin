public interface Base {
    fun test() = "OK"
}

open class Base2 : Base {
    override fun test() = "OK2"
}

class Delegate : Base

fun box(): String {

    object : Base2(), Base by Delegate() {
        override fun test() = "OK"
    }

    return "OK"
}
