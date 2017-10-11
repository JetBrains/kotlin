fun box(): String {
    var s = ""
    var foo = "O"

    fun foo(x: String) {
        s += x
    }

    fun foo() {
        foo("K")
    }

    run {
        foo(foo) // 1st foo is a local fun, second is a captured local var
        foo()
    }

    return s
}