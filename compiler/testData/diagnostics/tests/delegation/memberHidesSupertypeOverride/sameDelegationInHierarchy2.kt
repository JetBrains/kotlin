public interface Base {
    fun test() = "Base"
}

public interface Derived : Base {
    override fun test() = "Derived"
}


class Delegate : Derived {
    override fun test() = "Delegate"
}

public open class MyClass : Base by Delegate()

fun box(): String {
    <!MANY_IMPL_MEMBER_NOT_IMPLEMENTED, DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE!>object<!> : MyClass(), Derived by Delegate() {
    }
    return "OK"
}

