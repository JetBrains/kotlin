// test for KT-5337
package test

annotation class A(val value: String)

@A(<error descr="[NULL_FOR_NONNULL_TYPE] ">null</error>)
fun foo() {}

@A(<error descr="[NULL_FOR_NONNULL_TYPE] ">null</error>)
class B
