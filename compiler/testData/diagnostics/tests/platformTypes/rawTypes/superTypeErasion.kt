// FIR_IDENTICAL
// FILE: Test.java
class BaseOperation<T extends Bar, L extends Foo<T>> {}

class Foo<E extends Bar> { }

class Bar {}

public class Test extends BaseOperation {}

// FILE: main.kt
fun main() {
    val x = Test()
}