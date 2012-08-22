package d

<!ILLEGAL_MODIFIER!>abstract<!> val a : Int = 1

<!ILLEGAL_MODIFIER!>override<!> val c : Int = 1

<!ILLEGAL_MODIFIER!>final<!> fun foo() = 2

<!ILLEGAL_MODIFIER!>abstract<!> fun baz() = 2

class T {}
<!ILLEGAL_MODIFIER!>override<!> fun T.bar() = 2

