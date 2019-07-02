// KOTLIN_CONFIGURATION_FLAGS: +JVM.DISABLE_PARAM_ASSERTIONS
// IGNORE_BACKEND: JVM_IR

class A<T> {
    fun add(element: T) {}
}

public fun <R> foo(x: MutableCollection<in R>, block: () -> R) {
    x.add(block())
}

// 0 kotlin/jvm/internal/Intrinsics
