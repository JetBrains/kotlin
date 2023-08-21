package test

val myProperty: Int = 10

class MyClass(myProperty: Int) {
    val foo: Int = <expr>test.myProperty</expr>
}