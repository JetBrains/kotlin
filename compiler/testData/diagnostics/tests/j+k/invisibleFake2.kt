// ISSUE: KT-63242, KT-66324
// SCOPE_DUMP: D:foo
// FIR_DUMP

// FILE: box.kt

private open class C : B() {
    override fun foo(d: Any?): String = "C"
}

fun box(): String =
    D().<!INVISIBLE_MEMBER!>foo<!>("s")

// FILE: A.java
interface A {
    String foo(Object value);
}

abstract class B implements A {
    public String foo(String value) {
        return "B";
    }
}

class D extends C {
}
