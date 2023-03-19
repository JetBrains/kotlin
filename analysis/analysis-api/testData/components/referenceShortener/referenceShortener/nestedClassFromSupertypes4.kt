// FILE: main.kt
interface MyInterface {
    class Nested

    <expr>val prop: MyInterface.Nested get() = MyInterface.Nested()</expr>
}
