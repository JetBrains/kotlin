// KOTLIN_CONFIGURATION_FLAGS: +JVM.DISABLE_OPTIMIZATION

fun bar() {}

fun foo() {
    bar()
}

// 0 Unit\.INSTANCE
// 0 POP