// !WITH_NEW_INFERENCE
val receiver = { Int.(<!SYNTAX!><!>) <!SYNTAX!>-><!> }
val receiverWithParameter = { Int.<!ILLEGAL_SELECTOR!>(<!UNRESOLVED_REFERENCE!>a<!>)<!> <!SYNTAX!>-><!> }

val receiverAndReturnType = { Int.(<!SYNTAX!><!>)<!SYNTAX!>: Int ->  5<!> }
val receiverAndReturnTypeWithParameter = { Int.(<!UNRESOLVED_REFERENCE!>a<!><!SYNTAX!><!SYNTAX!><!>: Int): Int ->  5<!> }

val returnType = { (<!SYNTAX!><!>): Int -> 5 }
val returnTypeWithParameter = { (<!COMPONENT_FUNCTION_MISSING!><!UNUSED_DESTRUCTURED_PARAMETER_ENTRY!>b<!>: Int<!>): Int -> 5 }

val receiverWithFunctionType = { ((Int)<!SYNTAX!><!> <!SYNTAX!>-> Int).() -><!> }

val parenthesizedParameters = { <!CANNOT_INFER_PARAMETER_TYPE!>(<!NI;TYPE_MISMATCH!><!UNUSED_DESTRUCTURED_PARAMETER_ENTRY!>a<!>: Int<!>)<!> -> }
val parenthesizedParameters2 = { <!CANNOT_INFER_PARAMETER_TYPE!>(<!UNUSED_DESTRUCTURED_PARAMETER_ENTRY!>b<!>)<!> -> }

val none = { -> }


val parameterWithFunctionType = { a: ((Int) -> Int) -> <!SYNTAX!><!>} // todo fix parser

val newSyntax = { <!UNUSED_ANONYMOUS_PARAMETER!>a<!>: Int -> }
val newSyntax1 = { <!CANNOT_INFER_PARAMETER_TYPE, UNUSED_ANONYMOUS_PARAMETER!>a<!>, <!CANNOT_INFER_PARAMETER_TYPE, UNUSED_ANONYMOUS_PARAMETER!>b<!> -> }
val newSyntax2 = { <!UNUSED_ANONYMOUS_PARAMETER!>a<!>: Int, <!UNUSED_ANONYMOUS_PARAMETER!>b<!>: Int -> }
val newSyntax3 = { <!CANNOT_INFER_PARAMETER_TYPE, UNUSED_ANONYMOUS_PARAMETER!>a<!>, <!UNUSED_ANONYMOUS_PARAMETER!>b<!>: Int -> }
val newSyntax4 = { <!UNUSED_ANONYMOUS_PARAMETER!>a<!>: Int, <!CANNOT_INFER_PARAMETER_TYPE, UNUSED_ANONYMOUS_PARAMETER!>b<!> -> }
