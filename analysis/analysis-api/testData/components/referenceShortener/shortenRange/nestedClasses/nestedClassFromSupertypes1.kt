// FILE: main.kt

interface MyInterface {
    class Nested
}

class Foo : MyInterface {
    <expr>val prop: MyInterface.Nested = MyInterface.Nested()</expr>
}
