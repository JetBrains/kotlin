interface Base {
    fun test() = "Base"
}

class Delegate : Base

abstract class Middle : Base {
    override fun test() = "MyClass"
}

abstract class MyClass : Middle()

<!DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE!>class A<!> : MyClass(), Base by Delegate()
