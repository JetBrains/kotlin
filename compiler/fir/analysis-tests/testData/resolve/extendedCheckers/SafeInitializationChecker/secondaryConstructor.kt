class A {
    <!ACCESS_TO_UNINITIALIZED_VALUE!>val xx: String<!>

    constructor(x: String) {
        xx = x
    }

    constructor() {
        xx = foo()
    }

    fun foo() = x
}


