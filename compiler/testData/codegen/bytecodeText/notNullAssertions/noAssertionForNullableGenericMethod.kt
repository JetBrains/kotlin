// KOTLIN_CONFIGURATION_FLAGS: +JVM.DISABLE_PARAM_ASSERTIONS
// IGNORE_BACKEND: JVM_IR

fun <T> foo(a: List<T>) {
    val t: T = a.get(0)
}

// 0 kotlin/jvm/internal/Intrinsics
