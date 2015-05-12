annotation class B

class A {
   <!ILLEGAL_ANNOTATION_KEYWORD!>annotation<!> companion object {}
}

<!ILLEGAL_ANNOTATION_KEYWORD!>annotation<!> object O {}

<!ILLEGAL_ANNOTATION_KEYWORD!>annotation<!> interface T {}

<!ILLEGAL_ANNOTATION_KEYWORD!>annotation<!> fun f() = 0

<!ILLEGAL_ANNOTATION_KEYWORD!>annotation<!> val x = 0

<!ILLEGAL_ANNOTATION_KEYWORD!>annotation<!> var y = 0
