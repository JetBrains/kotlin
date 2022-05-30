// WITH_STDLIB

// KT-13592
class A {
    <!ACCESS_TO_UNINITIALIZED_VALUE!>var value1: String<!>
        set(value) {
            val f: String = field
            field = f.substring(1)
        }

    constructor() {
        value1 = ""
    }
}

class B {
    <!ACCESS_TO_UNINITIALIZED_VALUE!>val s: String<!>
        get(): String {
            val a = field.substring(1)
            return a
        }

    fun bar() = s

    init {
        s = bar()
    }
}
