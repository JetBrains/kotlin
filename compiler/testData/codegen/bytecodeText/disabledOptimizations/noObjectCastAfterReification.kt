// KOTLIN_CONFIGURATION_FLAGS: +JVM.DISABLE_OPTIMIZATION

inline fun <reified T> foo(s: Any) {
    s as T
}

fun main() {
    foo<String>("123")
}

// only one checkcast in reified function
// 1 CHECKCAST java/lang/Object