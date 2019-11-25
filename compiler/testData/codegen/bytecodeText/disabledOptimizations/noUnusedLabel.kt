// KOTLIN_CONFIGURATION_FLAGS: +JVM.DISABLE_OPTIMIZATION

fun bar() {}

fun foo(a: Boolean) { if (a) { foo(a) } }

// method start
// 5 L0

//return
// 3 L1

// method end
// 2 L2

// 0 L3
