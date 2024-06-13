// TARGET_BACKEND: JVM_IR
// ISSUE: KT-63242

// FILE: box.kt

private open class C : A<Double?>, B, BImpl() {
    override fun foo(d: Double?): String = "Fail: C"
}

fun box(): String = C().foo(0.0)

// FILE: A.java
interface A<T> {
    String foo(T value);
}

// FILE: B.java
interface B {
    String foo(double value);
}

// FILE: BImpl.java
class BImpl {
    public String foo(double value) {
        return "OK";
    }
}
