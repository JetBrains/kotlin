fun toInt(s: Number): Int {
    <caret>if (s is Int) {
        foo()
    }
    else {
        return -1
    }

    // code below will be lost!
    bar()
    // before return
    return s
}

fun foo() {}
fun bar() {}
