package c

import b.B

fun bar(b: B) {
    if (true) {
        b.foo()
    } else {
        b.bar()
    }
}
