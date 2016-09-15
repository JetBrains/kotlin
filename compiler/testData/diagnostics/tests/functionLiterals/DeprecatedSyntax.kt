val receiver = { Int.(<!SYNTAX!><!>) <!SYNTAX!>-><!> }
val receiverWithParameter = { Int.<!ILLEGAL_SELECTOR!>(<!UNRESOLVED_REFERENCE!>a<!>)<!> <!SYNTAX!>-><!> }

val receiverAndReturnType = { Int.(<!SYNTAX!><!>)<!SYNTAX!>: Int ->  5<!> }
val receiverAndReturnTypeWithParameter = { Int.(<!UNRESOLVED_REFERENCE!>a<!><!SYNTAX!><!SYNTAX!><!>: Int): Int ->  5<!> }

val returnType = { (<!SYNTAX!><!>): Int -> 5 }
val returnTypeWithParameter = { (<!COMPONENT_FUNCTION_MISSING!>b: Int<!>): Int -> 5 }

val receiverWithFunctionType = { ((Int)<!SYNTAX!><!> <!SYNTAX!>-> Int).() -><!> }

val parenthesizedParameters = { <!CANNOT_INFER_PARAMETER_TYPE!>(a: Int)<!> -> }
val parenthesizedParameters2 = { <!CANNOT_INFER_PARAMETER_TYPE!>(b)<!> -> }

val none = { -> }


val parameterWithFunctionType = { a: ((Int) -> Int) -> <!SYNTAX!><!>} // todo fix parser

val newSyntax = { a: Int -> }
val newSyntax1 = { <!CANNOT_INFER_PARAMETER_TYPE!>a<!>, <!CANNOT_INFER_PARAMETER_TYPE!>b<!> -> }
val newSyntax2 = { a: Int, b: Int -> }
val newSyntax3 = { <!CANNOT_INFER_PARAMETER_TYPE!>a<!>, b: Int -> }
val newSyntax4 = { a: Int, <!CANNOT_INFER_PARAMETER_TYPE!>b<!> -> }
