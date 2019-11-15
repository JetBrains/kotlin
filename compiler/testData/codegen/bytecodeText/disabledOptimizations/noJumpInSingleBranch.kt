// KOTLIN_CONFIGURATION_FLAGS: +JVM.DISABLE_OPTIMIZATION

fun consume(i: Int) {}

fun foo(a: Boolean) {
    var b = 1
    if (a) {
        b = 2
    }

    consume(b)
}

// 0 GOTO
// 1 IF