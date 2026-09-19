// IGNORE_TREE_ACCESS: KT-64899

interface MyInterface {
    fun foo()
}

val localProperty: Any = object : MyInterface {
    override fun foo() {}
}

localProperty as MyInterface

class MyClass : MyInterface by localProperty
