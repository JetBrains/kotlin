val receiver = { <!DEPRECATED_LAMBDA_SYNTAX!>Int.()<!> -> }
val receiverWithParameter = { <!DEPRECATED_LAMBDA_SYNTAX!>Int.(<!CANNOT_INFER_PARAMETER_TYPE!>a<!>)<!> -> }

val receiverAndReturnType = { <!DEPRECATED_LAMBDA_SYNTAX!>Int.(): Int<!> ->  5 }
val receiverAndReturnTypeWithParameter = { <!DEPRECATED_LAMBDA_SYNTAX!>Int.(a: Int): Int<!> ->  5 }

val returnType = { <!DEPRECATED_LAMBDA_SYNTAX!>(): Int<!> -> 5 }
val returnTypeWithParameter = { <!DEPRECATED_LAMBDA_SYNTAX!>(b: Int): Int<!> -> 5 }

val receiverWithFunctionType = { <!DEPRECATED_LAMBDA_SYNTAX!>((Int) -> Int).()<!> -> }

val parenthesizedParameters = { <!DEPRECATED_LAMBDA_SYNTAX!>(a: Int)<!> -> }
val parenthesizedParameters2 = { <!DEPRECATED_LAMBDA_SYNTAX!>(<!CANNOT_INFER_PARAMETER_TYPE!>b<!>)<!> -> }

val none = { -> }


val parameterWithFunctionType = { <!UNRESOLVED_REFERENCE!>a<!>: ((Int) -> Int) -> <!SYNTAX!><!>} // todo fix parser

val newSyntax = { a: Int -> }
val newSyntax1 = { <!CANNOT_INFER_PARAMETER_TYPE!>a<!>, <!CANNOT_INFER_PARAMETER_TYPE!>b<!> -> }
val newSyntax2 = { a: Int, b: Int -> }
val newSyntax3 = { <!CANNOT_INFER_PARAMETER_TYPE!>a<!>, b: Int -> }
val newSyntax4 = { a: Int, <!CANNOT_INFER_PARAMETER_TYPE!>b<!> -> }