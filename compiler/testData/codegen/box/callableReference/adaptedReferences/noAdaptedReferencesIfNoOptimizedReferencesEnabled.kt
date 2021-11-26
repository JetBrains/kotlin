// TARGET_BACKEND: JVM
// WITH_STDLIB
// NO_OPTIMIZED_CALLABLE_REFERENCES

class A {
    fun target(): Int = 42
}

fun foo(f: () -> Unit): Any = f

fun box(): String {
    val o = foo(A()::target)
    if (o is kotlin.jvm.internal.AdaptedFunctionReference ||
        o !is kotlin.jvm.internal.FunctionReference)
        return "Fail: we shouldn't generate reference to AdaptedFunctionReference if -Xno-optimized-callable-references is enabled"

    return "OK"
}
