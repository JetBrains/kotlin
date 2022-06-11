// KT-12152
class Test(extFun: Test.() -> String) {
    // This test also works for implicit this, but you need to find a symbol to attach the warning
    val x = <!VALUE_CANNOT_BE_PROMOTED!>this<!>.extFun()
}

class B {
    val kaboom = Test { x }.x
}
// KT-17382
class C {
    val x: String = { y }()
    <!ACCESS_TO_UNINITIALIZED_VALUE!>val y: String = x<!>
}
