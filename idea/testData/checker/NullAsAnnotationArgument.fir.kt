// test for KT-5337
package test

annotation class A(val value: String)

<error descr="[INAPPLICABLE_CANDIDATE] Inapplicable candidate(s): test/A.A">@A(null)</error>
fun foo() {}

<error descr="[INAPPLICABLE_CANDIDATE] Inapplicable candidate(s): test/A.A">@A(null)</error>
class B
