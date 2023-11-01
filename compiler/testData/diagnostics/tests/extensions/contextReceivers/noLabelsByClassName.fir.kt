// ISSUE: KT-63068
fun Int.f() {
    this@Int
}

var Int.p: Int
    get() {
        this@Int
        <!RETURN_NOT_ALLOWED!>return@p<!> 42
    }
    set(value) {
        this@Int
    }

class X {
    var Int.p: Int
        get() {
            this@Int
            <!RETURN_NOT_ALLOWED!>return@p<!> 42
        }
        set(value) {
            this@Int
        }

    fun Int.f() {
        this@Int
    }
}
