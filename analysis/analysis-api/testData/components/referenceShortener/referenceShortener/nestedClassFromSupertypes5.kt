// FILE: main.kt
interface MyInterface {
    class Nested
}

fun MyInterface.foo() {
    <expr>val prop: MyInterface.Nested = MyInterface.Nested()</expr>
}
