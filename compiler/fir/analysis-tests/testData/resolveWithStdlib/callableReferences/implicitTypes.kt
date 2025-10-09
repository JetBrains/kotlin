// RUN_PIPELINE_TILL: FRONTEND
fun <T, R> use(x: (T) -> R): (T) -> R = x

fun foo() = use(::bar)
fun bar(x: String) = 1

fun loop1() = <!CANNOT_INFER_PARAMETER_TYPE("T"), CANNOT_INFER_PARAMETER_TYPE("R")!>use<!>(::<!INAPPLICABLE_CANDIDATE("fun loop2(): ??? (Recursive implicit type)")!>loop2<!>)
fun loop2() = <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>loop1()<!>

/* GENERATED_FIR_TAGS: callableReference, functionDeclaration, functionalType, integerLiteral, nullableType,
typeParameter */
