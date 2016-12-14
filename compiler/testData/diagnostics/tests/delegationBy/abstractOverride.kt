public interface Base {
    fun test() = "OK"
}


abstract class Impl : Base {
    override abstract fun test(): String
}

class Delegate : Base


fun box(): String {
    object : Impl(), Base by Delegate() {
    }

    return "OK"
}
