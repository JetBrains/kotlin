// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: J.java
import kotlin.reflect.KCallable;
import kotlin.jvm.functions.*;

public class J {
    public final int value;

    public static void checkInJava(KCallable<?> ref) {
        if (!(ref instanceof Function0)) throw new AssertionError("Should be a Function0 in Java: " + ref);
        if (!(ref instanceof Function1)) throw new AssertionError("Should be a Function1 in Java: " + ref);
        if (!(ref instanceof Function2)) throw new AssertionError("Should be a Function2 in Java: " + ref);;
        if (!(ref instanceof Function3)) throw new AssertionError("Should be a Function3 in Java: " + ref);;
    }

    public J(int value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "value=" + String.valueOf(value);
    }
}

// FILE: JAnno.java
public @interface JAnno {
    int value();
}

// FILE: test.kt
import kotlin.reflect.KCallable

class C(val value: Int) {
    override fun toString(): String = "value=$value"
}

annotation class Anno(val value: Int)

private fun check(ref: KCallable<*>) {
    if (ref is Function0<*>) error("Should not be a Function0: $ref")
    if (ref !is Function1<*, *>) error("Should not be a Function1: $ref")
    if (ref is Function2<*, *, *>) error("Should be a Function2: $ref")
    if (ref is Function3<*, *, *, *>) error("Should not be a Function3: $ref")
    if (ref is Function25<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *>) error("Should not be a Function25: $ref")
    J.checkInJava(ref)

    ref as? Function1<Int, Any> ?: error("Should be castable to Function1: $ref")

    if ("value=42" !in ref(42).toString()) error("Incorrect result: ${ref(42)}")
}

fun box(): String {
    check(C::class.constructors.single())
    check(J::class.constructors.single())

    check(Anno::class.constructors.single())
    check(JAnno::class.constructors.single())

    return "OK"
}
