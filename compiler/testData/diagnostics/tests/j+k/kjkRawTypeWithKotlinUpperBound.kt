// ISSUE: KT-66158
// FILE: Java1.java
public class Java1 extends KotlinClass { }

// FILE: 1.kt
open class KotlinClass<T : Number> {
    open fun bar(): T? {
        return null!!
    }
}

class A : Java1()

fun test(a: A) {
    val k: Number = <!TYPE_MISMATCH!>a.bar()<!>
}