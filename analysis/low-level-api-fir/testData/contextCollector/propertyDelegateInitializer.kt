package test

interface MyInterface {
    operator fun getValue(thisRef: Any, property: Any): MyInterface
}

class Foo(constructorParam: MyInterface) {
    val regularProperty: MyInterface

    val property: MyInterface by <expr>constructorParam</expr>
}