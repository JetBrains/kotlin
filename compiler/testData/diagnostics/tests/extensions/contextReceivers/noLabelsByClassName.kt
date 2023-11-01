// ISSUE: KT-63068
fun Int.f() {
    this<!UNRESOLVED_REFERENCE!>@Int<!>
}

var Int.p: Int
    get() {
        this<!UNRESOLVED_REFERENCE!>@Int<!>
        return<!UNRESOLVED_REFERENCE!>@p<!> 42
    }
    set(value) {
        this<!UNRESOLVED_REFERENCE!>@Int<!>
    }

class X {
    var Int.p: Int
        get() {
            this<!UNRESOLVED_REFERENCE!>@Int<!>
            return<!UNRESOLVED_REFERENCE!>@p<!> 42
        }
        set(value) {
            this<!UNRESOLVED_REFERENCE!>@Int<!>
        }

    fun Int.f() {
        this<!UNRESOLVED_REFERENCE!>@Int<!>
    }
}
