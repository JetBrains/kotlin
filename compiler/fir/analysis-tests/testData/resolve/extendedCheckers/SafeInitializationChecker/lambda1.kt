// KT-12152
class Test(extFun: Test.() -> String) {
    val x = <!VALUE_CANNOT_BE_PROMOTED!>extFun()<!>
}

val kaboom = Test { x }.x

// KT-17382
class A {
    <!ACCESS_TO_UNINITIALIZED_VALUE!>val x: String = { y }()<!>
    <!ACCESS_TO_UNINITIALIZED_VALUE!>val y: String = x<!>
}
