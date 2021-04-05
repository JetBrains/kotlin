// test for KT-5337
package test

annotation class A(val value: String)

@A(<error descr="[ARGUMENT_TYPE_MISMATCH] Argument type mismatch: actual type is kotlin/Nothing? but kotlin/String was expected">null</error>)
fun foo() {}

@A(<error descr="[ARGUMENT_TYPE_MISMATCH] Argument type mismatch: actual type is kotlin/Nothing? but kotlin/String was expected">null</error>)
class B
