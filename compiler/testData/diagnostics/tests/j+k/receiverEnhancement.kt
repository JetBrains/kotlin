// FIR_IDENTICAL
// FILE: J.java
public class J extends Foo {
    @Override
    public <T> T bar(String $this$bar) {
        super.bar($this$bar);
    }
}

// FILE: test.kt
open class Foo {
    fun <T> String.bar(): T {
        @Suppress("UNCHECKED_CAST")
        return null as T
    }
}

fun J.test(s1: String?, s2: String) {
    s1<!UNSAFE_CALL!>.<!>bar<Int>()
    s1?.bar<Int>()
    s2.bar<Int>()
    s2<!UNNECESSARY_SAFE_CALL!>?.<!>bar<Int>()
}
