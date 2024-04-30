// FIR_IDENTICAL

// FILE: BaseOperation.java
class BaseOperation<T extends Bar, L extends Foo<T>> {}

// FILE: Foo.java
class Foo<E extends Bar> { }

// FILE: Bar.java
class Bar {}

// FILE: Test.java
public class Test extends BaseOperation {}

// FILE: main.kt
fun main() {
    val x = Test()
}