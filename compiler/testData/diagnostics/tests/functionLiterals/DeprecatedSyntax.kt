val receiver = { Int.(<!SYNTAX!><!>) <!SYNTAX!>-><!> }
val receiverWithParameter = { Int.<!ILLEGAL_SELECTOR!>(<!DEBUG_INFO_MISSING_UNRESOLVED!>a<!>)<!> <!SYNTAX!>-><!> }

val receiverAndReturnType = { Int.(<!SYNTAX!><!>): Int <!SYNTAX!>->  5<!> }
val receiverAndReturnTypeWithParameter = { Int.<!ILLEGAL_SELECTOR!>(<!DEBUG_INFO_MISSING_UNRESOLVED!>a<!>: <!DEBUG_INFO_MISSING_UNRESOLVED!>Int<!>)<!>: Int <!SYNTAX!>->  5<!> }

val returnType = { (<!SYNTAX!><!>): Int <!SYNTAX!>-> 5<!> }
val returnTypeWithParameter = { (<!UNRESOLVED_REFERENCE!>b<!>: Int)<!DEPRECATED_STATIC_ASSERT!>: Int<!> <!SYNTAX!>-> 5<!> }

val receiverWithFunctionType = { ((Int)<!SYNTAX!><!> <!SYNTAX!>-> Int).() -><!> }

val parenthesizedParameters = { (<!UNRESOLVED_REFERENCE!>a<!>: Int) <!SYNTAX!>-><!> }
val parenthesizedParameters2 = { (<!UNRESOLVED_REFERENCE!>b<!>) <!SYNTAX!>-><!> }

val none = { -> }


val parameterWithFunctionType = { <!UNRESOLVED_REFERENCE!>a<!>: ((Int) -> Int) -> <!SYNTAX!><!>} // todo fix parser

val newSyntax = { a: Int -> }
val newSyntax1 = { <!CANNOT_INFER_PARAMETER_TYPE!>a<!>, <!CANNOT_INFER_PARAMETER_TYPE!>b<!> -> }
val newSyntax2 = { a: Int, b: Int -> }
val newSyntax3 = { <!CANNOT_INFER_PARAMETER_TYPE!>a<!>, b: Int -> }
val newSyntax4 = { a: Int, <!CANNOT_INFER_PARAMETER_TYPE!>b<!> -> }