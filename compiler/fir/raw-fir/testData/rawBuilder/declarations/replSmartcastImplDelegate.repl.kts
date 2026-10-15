interface MyInterface {
    fun foo()
}

val localProperty: Any = object : MyInterface {
    override fun foo() {}
}

localProperty as MyInterface

class MyClass : MyInterface by localProperty
