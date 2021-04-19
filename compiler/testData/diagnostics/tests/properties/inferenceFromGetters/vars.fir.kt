// !CHECK_TYPE
var x
    get() = 1
    set(q) {
        q checkType { _<Int>() }
    }

<!MUST_BE_INITIALIZED!>var noSetter<!>
    get() = 1


fun foo() {
    x checkType { _<Int>() }
    noSetter checkType { _<Int>() }

    x = 1
    x = <!ASSIGNMENT_TYPE_MISMATCH!>""<!>

    noSetter = 2
    noSetter = <!ASSIGNMENT_TYPE_MISMATCH!>""<!>
}
