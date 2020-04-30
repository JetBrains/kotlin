// TARGET_BACKEND: JVM
// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME
// KOTLIN_CONFIGURATION_FLAGS: +JVM.NO_OPTIMIZED_CALLABLE_REFERENCES

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
