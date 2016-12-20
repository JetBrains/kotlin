public interface Base {
    fun test() = "OK"
}

public interface Base2 : Base {
    override fun test() = "OK2"
}

class Delegate : Base2

fun box(): String {
    object : Base, Base2 by Delegate() {
    }

    <!DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE!>object<!> : Base2, Base by Delegate() {
    }

    return "OK"
}
