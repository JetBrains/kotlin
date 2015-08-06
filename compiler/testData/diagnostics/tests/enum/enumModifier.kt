enum class B {}

class A {
   <!WRONG_MODIFIER_TARGET!>enum<!> companion object {}
}

<!WRONG_MODIFIER_TARGET!>enum<!> object O {}

<!WRONG_MODIFIER_TARGET!>enum<!> interface T {}

<!WRONG_MODIFIER_TARGET!>enum<!> fun f() = 0

<!WRONG_MODIFIER_TARGET!>enum<!> val x = 0

<!WRONG_MODIFIER_TARGET!>enum<!> var y = 0