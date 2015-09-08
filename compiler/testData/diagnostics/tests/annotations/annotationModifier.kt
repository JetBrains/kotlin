annotation class B

class A {
   <!WRONG_MODIFIER_TARGET!>annotation<!> companion object {}
}

<!WRONG_MODIFIER_TARGET!>annotation<!> object O {}

<!WRONG_MODIFIER_TARGET!>annotation<!> interface T {}

<!WRONG_MODIFIER_TARGET!>annotation<!> fun f() = 0

<!WRONG_MODIFIER_TARGET!>annotation<!> val x = 0

<!WRONG_MODIFIER_TARGET!>annotation<!> var y = 0