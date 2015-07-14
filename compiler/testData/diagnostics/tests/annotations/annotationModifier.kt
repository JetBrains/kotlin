annotation class B

class A {
   <!WRONG_ANNOTATION_TARGET!>annotation<!> companion object {}
}

<!WRONG_ANNOTATION_TARGET!>annotation<!> object O {}

<!WRONG_ANNOTATION_TARGET!>annotation<!> interface T {}

<!WRONG_ANNOTATION_TARGET!>annotation<!> fun f() = 0

<!WRONG_ANNOTATION_TARGET!>annotation<!> val x = 0

<!WRONG_ANNOTATION_TARGET!>annotation<!> var y = 0