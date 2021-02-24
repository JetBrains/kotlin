// !CHECK_TYPE
var x
    get() = 1
    set(q) {
        q checkType { _<Int>() }
    }

var noSetter
    get() = 1


fun foo() {
    x checkType { _<Int>() }
    noSetter checkType { _<Int>() }

    x = 1
    x = ""

    noSetter = 2
    noSetter = ""
}
