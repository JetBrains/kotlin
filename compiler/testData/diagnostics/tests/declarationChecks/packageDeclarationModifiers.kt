package d

<!WRONG_MODIFIER_TARGET!>abstract<!> val a : Int = 1

<!WRONG_MODIFIER_TARGET!>override<!> val c : Int = 1

<!WRONG_MODIFIER_TARGET!>final<!> fun foo() = 2

<!WRONG_MODIFIER_TARGET!>abstract<!> fun baz() = 2

class T {}
<!WRONG_MODIFIER_TARGET!>override<!> fun T.bar() = 2