package skipSimpleGetterMethodWithProperty

fun main(args: Array<String>) {
    //Breakpoint!
    test()
    foo()
}

fun test() {
    var a = 12
    foo()
}

fun foo() {}

// SKIP_GETTERS: true