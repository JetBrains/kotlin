// FILE: JavaClass.java
class FooJava {
    String call() {}
}

// FILE: main.kt
fun text(foo: FooJava) {
    <expr>foo.call()</expr>
}