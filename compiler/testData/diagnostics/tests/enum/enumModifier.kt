enum class B {}

class A {
   <!ILLEGAL_ENUM_ANNOTATION!>enum<!> companion object {}
}

<!ILLEGAL_ENUM_ANNOTATION!>enum<!> object O {}

<!ILLEGAL_ENUM_ANNOTATION!>enum<!> interface T {}

<!ILLEGAL_ENUM_ANNOTATION!>enum<!> fun f() = 0

<!ILLEGAL_ENUM_ANNOTATION!>enum<!> val x = 0

<!ILLEGAL_ENUM_ANNOTATION!>enum<!> var y = 0
