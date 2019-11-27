// KOTLIN_CONFIGURATION_FLAGS: +JVM.DISABLE_OPTIMIZATION

fun consume(i: Int) {}

fun foo(a: String) {
    var b = 1
    if (a[1] == 'b') {
        b = 2
    } else if (a[0] == 'a') {
        b = 3
    }

    consume(b)
}

// 1 GOTO
// 2 IF