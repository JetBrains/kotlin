annotation class B

class A {
   annotation companion object {}
}

annotation object O {}

annotation interface T {}

<!WRONG_ANNOTATION_TARGET!>annotation<!> fun f() = 0

<!WRONG_ANNOTATION_TARGET!>annotation<!> val x = 0

<!WRONG_ANNOTATION_TARGET!>annotation<!> var y = 0