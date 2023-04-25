// ISSUE: KT-25808
// WITH_STDLIB

// B.java
public class B {
}

// test.kt
class A

fun main(args: Array<String>) {
    (1 to A()) == A()
    (1 to B()) == B()
}
