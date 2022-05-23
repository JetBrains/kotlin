fun foo(): String = "FAIL1"
fun exp_foo(): String = "FAIL2"

class A {
    fun foo(): String = "FAIL3"
    fun exp_foo(): String = "FAIL4"
}
