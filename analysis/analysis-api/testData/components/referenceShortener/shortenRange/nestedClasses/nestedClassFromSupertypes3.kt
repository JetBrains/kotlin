// FILE: main.kt
open class MyBaseClass {
    class Nested
}

class Foo : MyBaseClass() {
    <expr>val prop: MyBaseClass.Nested = MyBaseClass.Nested()</expr>
}
