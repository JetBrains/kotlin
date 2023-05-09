// !DIAGNOSTICS: -UNUSED_PARAMETER
class A(val w: Char) {
    val x: Int
    var y: Int
    val z: Int
    val v = -1

    <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>val uninitialized: Int<!>
    val overinitialized: Int

    constructor(): this('a') {
        y = 1

        <!VAL_REASSIGNMENT!>overinitialized<!> = 2
        <!VAL_REASSIGNMENT!>uninitialized<!> = 3
    }

    // anonymous
    init {
        x = 4
        z = 5
        overinitialized = 6
    }

    constructor(a: Int): this('b') {
        y = 7
    }

    // anonymous
    init {
        y = 8
    }
}
