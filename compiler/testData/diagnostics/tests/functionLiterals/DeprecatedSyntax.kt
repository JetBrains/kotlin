val receiver = { Int.(<!SYNTAX!><!>) <!SYNTAX!>-><!> }
val receiverWithParameter = { Int.<!ILLEGAL_SELECTOR!>(<!DEBUG_INFO_MISSING_UNRESOLVED!>a<!>)<!> <!SYNTAX!>-><!> }

val receiverAndReturnType = { Int.(<!SYNTAX!><!>)<!SYNTAX!>: Int ->  5<!> }
val receiverAndReturnTypeWithParameter = { Int.(<!DEBUG_INFO_MISSING_UNRESOLVED!>a<!><!SYNTAX!><!SYNTAX!><!>: Int): Int ->  5<!> }

val returnType = { (<!SYNTAX!><!>)<!SYNTAX!>: Int -> 5<!> }
val returnTypeWithParameter = { (<!UNRESOLVED_REFERENCE!>b<!><!SYNTAX!><!SYNTAX!><!>: Int): Int -> 5<!> }

val receiverWithFunctionType = { ((Int)<!SYNTAX!><!> <!SYNTAX!>-> Int).() -><!> }

val parenthesizedParameters = { (<!UNRESOLVED_REFERENCE!>a<!><!SYNTAX!><!SYNTAX!><!>: Int) -><!> }
val parenthesizedParameters2 = { (<!UNRESOLVED_REFERENCE!>b<!>) <!SYNTAX!>-><!> }

val none = { -> }


val parameterWithFunctionType = { <!UNRESOLVED_REFERENCE!>a<!><!SYNTAX!>: ((Int) -> Int) -><!> } // todo fix parser

val newSyntax = { a: Int -> }
val newSyntax1 = { <!CANNOT_INFER_PARAMETER_TYPE!>a<!>, <!CANNOT_INFER_PARAMETER_TYPE!>b<!> -> }
val newSyntax2 = { a: Int, b: Int -> }
val newSyntax3 = { <!CANNOT_INFER_PARAMETER_TYPE!>a<!>, b: Int -> }
val newSyntax4 = { a: Int, <!CANNOT_INFER_PARAMETER_TYPE!>b<!> -> }