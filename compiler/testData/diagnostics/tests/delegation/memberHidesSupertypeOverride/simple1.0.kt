// !LANGUAGE_VERSION: 1.0

public interface Base {
    fun getValue(): String

    fun test() = getValue()
}

class Delegate : Base {
    override fun getValue() = "Delegate"
}

public abstract class MyClass : Base {
    override fun test(): String {
        return "Class"
    }
}

<!DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE!>class A<!> : MyClass(), Base by Delegate() {
    override fun getValue() = "Delegate"
}