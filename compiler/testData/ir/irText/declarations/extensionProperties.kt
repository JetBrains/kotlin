// FIR_IDENTICAL

val String.test1 get() = 42

var String.test2
    get() = 42
    set(value) {}

class Host {
    val String.test3 get() = 42

    var String.test4
        get() = 42
        set(value) {}
}
