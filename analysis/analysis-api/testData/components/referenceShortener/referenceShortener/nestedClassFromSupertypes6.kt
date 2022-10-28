// FILE: main.kt
open class MyBaseClass {
    class Nested
}

fun MyBaseClass.foo() {
    <expr>val prop: MyBaseClass.Nested = MyBaseClass.Nested()</expr>
}
