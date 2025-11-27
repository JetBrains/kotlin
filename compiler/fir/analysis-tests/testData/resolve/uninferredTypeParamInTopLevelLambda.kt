// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTIC_ARGUMENTS
// ISSUE: KT-82737

fun expectString() {
    val withUnitReturn: String <!INITIALIZER_TYPE_MISMATCH("String; () -> Unit")!>=<!> {}
    val withIntReturn: String <!INITIALIZER_TYPE_MISMATCH("String; () -> Int")!>=<!> { 42 }
    val withParamAndIntReturn: String <!INITIALIZER_TYPE_MISMATCH("String; (??? (Unknown lambda parameter type)) -> ??? (Unknown lambda return type)")!>=<!> { <!VALUE_PARAMETER_WITHOUT_EXPLICIT_TYPE!>it<!> -> 42 }
    val withParamAnUnitReturn: String <!INITIALIZER_TYPE_MISMATCH("String; (??? (Unknown lambda parameter type)) -> ??? (Unknown lambda return type)")!>=<!> { <!VALUE_PARAMETER_WITHOUT_EXPLICIT_TYPE!>it<!> -> }
    val withParamReturn: String <!INITIALIZER_TYPE_MISMATCH("String; (??? (Unknown lambda parameter type)) -> ??? (Unknown lambda return type)")!>=<!> { <!VALUE_PARAMETER_WITHOUT_EXPLICIT_TYPE!>it<!> -> it }
}

fun free() {
    val withParamAndIntReturn = { <!VALUE_PARAMETER_WITHOUT_EXPLICIT_TYPE!>it<!> -> 42 }
    val withParamAnUnitReturn = { <!VALUE_PARAMETER_WITHOUT_EXPLICIT_TYPE!>it<!> -> }
    val withParamReturn = { <!VALUE_PARAMETER_WITHOUT_EXPLICIT_TYPE!>it<!> -> it }
}

/* GENERATED_FIR_TAGS: functionDeclaration, lambdaLiteral, localProperty, propertyDeclaration */
