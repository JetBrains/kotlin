interface Base {
    fun test() = "Base"
}

class Delegate : Base

abstract class Middle : Base {
    override fun test() = "MyClass"
}

abstract class MyClass : Middle()

class A : MyClass(), Base by Delegate()
