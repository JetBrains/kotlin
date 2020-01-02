// !CHECK_TYPE
var x
    get() = 1
    set(q) {
        q checkType { <!UNRESOLVED_REFERENCE!>_<!><Int>() }
    }

var noSetter
    get() = 1


fun foo() {
    x checkType { <!UNRESOLVED_REFERENCE!>_<!><Int>() }
    noSetter checkType { <!UNRESOLVED_REFERENCE!>_<!><Int>() }

    x = 1
    x = ""

    noSetter = 2
    noSetter = ""
}
