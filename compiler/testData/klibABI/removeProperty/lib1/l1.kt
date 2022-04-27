val foo: String get() = "FAIL1"
val exp_foo: String get() = "FAIL2"

class A {
    val foo: String get() = "FAIL3"
    val exp_foo: String get() = "FAIL4"
}

class B {
    val foo: String = "FAIL5"
    val exp_foo: String = "FAIL6"
}
