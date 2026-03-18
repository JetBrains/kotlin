// This test checks that functions obtained from Kotlin reflection can be cast to the corresponding function type with `invoke` of correct
// arity. The way it's implemented is that any `KCallable` implementation inherits from `FunctionWithAllInvokes`, which is a subclass of all
// function types. And `obj is Function{n}` is generated in the Kotlin compiler as a call to an intrinsic which checks if `obj` is an
// instance of `FunctionBase`, gets its arity, and compares it with `n`. This allows to cast functions obtained from reflection (i.e.
// `KClass.members`) to corresponding `Function{n}` types, and use `invoke` to call them, as if those were lambdas or function expressions.
// For more information, see `spec-docs/function-types.md`.

// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: J.java
import kotlin.reflect.KCallable;
import kotlin.jvm.functions.*;

public class J {
    public static void checkInJava(KCallable<?> ref) {
        // KCallable implementations inherit from all function interfaces on JVM.
        if (!(ref instanceof Function0)) throw new AssertionError("Should be a Function0 in Java: " + ref);
        if (!(ref instanceof Function1)) throw new AssertionError("Should be a Function1 in Java: " + ref);
        if (!(ref instanceof Function2)) throw new AssertionError("Should be a Function2 in Java: " + ref);
        if (!(ref instanceof Function3)) throw new AssertionError("Should be a Function3 in Java: " + ref);
    }

    public int javaMethod(int x) {
        return x + 1;
    }

    public static int javaStaticMethod(Object o, int x) {
        return x + 1;
    }
}

// FILE: test.kt
import kotlin.reflect.KCallable

class C {
    fun kotlinFun(x: Int): Int = x + 1
}

private fun check(ref: KCallable<*>, instance: Any?) {
    // In Kotlin, `is Function{n}` is generated as a call to `Intrinsics.isFunctionOfArity`, which uses the "real" arity of a function object.
    if (ref is Function0<*>) error("Should not be a Function0: $ref")
    if (ref is Function1<*, *>) error("Should not be a Function1: $ref")
    if (ref !is Function2<*, *, *>) error("Should be a Function2: $ref")
    if (ref is Function3<*, *, *, *>) error("Should not be a Function3: $ref")
    if (ref is Function25<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *>) error("Should not be a Function25: $ref")
    J.checkInJava(ref)

    ref as? Function2<Any?, Int, Int> ?: error("Should be castable to Function2: $ref")

    if (ref(instance, 42) != 43) error("Incorrect result: ${ref(instance, 42)}")
}

fun box(): String {
    check(C::class.members.single { it.name == "kotlinFun" }, C())
    check(J::class.members.single { it.name == "javaMethod" }, J())
    check(J::class.members.single { it.name == "javaStaticMethod" }, null)

    return "OK"
}
