// !WITH_NEW_INFERENCE
val receiver = { Int.(<!SYNTAX!><!>) <!SYNTAX!>-><!> }
val receiverWithParameter = { Int.(<!UNRESOLVED_REFERENCE!>a<!>) <!SYNTAX!>-><!> }

val receiverAndReturnType = { Int.(<!SYNTAX!><!>)<!SYNTAX!>: Int ->  5<!> }
val receiverAndReturnTypeWithParameter = { Int.(<!UNRESOLVED_REFERENCE!>a<!><!SYNTAX!><!SYNTAX!><!>: Int): Int ->  5<!> }

val returnType = { (<!SYNTAX!><!>): Int -> 5 }
val returnTypeWithParameter = { (<!UNRESOLVED_REFERENCE!>b: Int<!>): Int -> 5 }

val receiverWithFunctionType = { ((Int)<!SYNTAX!><!> <!SYNTAX!>-> Int).() -><!> }

val parenthesizedParameters = { (<!UNRESOLVED_REFERENCE!>a: Int<!>) -> }
val parenthesizedParameters2 = { (<!UNRESOLVED_REFERENCE!>b<!>) -> }

val none = { -> }


val parameterWithFunctionType = { a: ((Int) -> Int) -> <!SYNTAX!><!>} // todo fix parser

val newSyntax = { a: Int -> }
val newSyntax1 = { a, b -> }
val newSyntax2 = { a: Int, b: Int -> }
val newSyntax3 = { a, b: Int -> }
val newSyntax4 = { a: Int, b -> }
