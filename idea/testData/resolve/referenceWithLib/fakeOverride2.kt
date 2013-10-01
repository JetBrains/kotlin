package test

import dependency.A

public open class B(): A() {
}

fun test(b: B) {
    b.<caret>f()
}

// REF: (in dependency.A).f()