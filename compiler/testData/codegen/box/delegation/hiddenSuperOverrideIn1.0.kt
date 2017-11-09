// LANGUAGE_VERSION: 1.0

public interface Base {
    fun test() = "base fail"
}

public interface Base2 : Base {
    override fun test() = "base 2fail"
}

class Delegate : Base {
    override fun test(): String {
        return "OK"
    }
}

fun box(): String {
    return object : Base2, Base by Delegate() {
    }.test()
}
