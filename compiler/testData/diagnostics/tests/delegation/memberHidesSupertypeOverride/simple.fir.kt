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

class A : MyClass(), Base by Delegate() {
    override fun getValue() = "Delegate"
}