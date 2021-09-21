// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM
// JVM_TARGET: 1.8
// FILE: JavaInterface.java

public interface JavaInterface<T> {
    default T foo(T param) {
        return param;
    }
}

// FILE: JavaDerived.java
public interface JavaDerived extends JavaInterface<Derived> {

}

// FILE: Kotlin.kt
class Derived(val value: String)

class Test : JavaDerived {
    override fun foo(a: Derived?): Derived {
        return super.foo(a)
    }
}

fun box(): String {
    return Test().foo(Derived("OK")).value
}
