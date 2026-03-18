// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CollectionLiterals
// WITH_STDLIB

class LambdaCollection<T> {
    companion object {
        operator fun <T> of(vararg block: () -> LambdaCollection<T>) = LambdaCollection<T>()
    }
}

class C1 {
    companion object {
        operator fun of(vararg block: () -> Int) = C1()
    }
}

class C2 {
    companion object {
        operator fun of(vararg block: () -> String) = C2()
    }
}

fun f1(x: LambdaCollection<Int>) { }
fun f1(x: LambdaCollection<String>) { }

fun f2(c1: C1) { }
fun f2(c2: C2) { }

fun f3(x: List<() -> Int>) { }
fun f3(x: List<() -> String>) { }

fun test() {
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>f1<!>([{ LambdaCollection<Int>() }])
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>f1<!>(<!CANNOT_INFER_PARAMETER_TYPE!>[<!CANNOT_INFER_IT_PARAMETER_TYPE!>{ <!CANNOT_INFER_PARAMETER_TYPE!>LambdaCollection<!>() }<!>]<!>)
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>f1<!>([{ LambdaCollection<Int>() }, { LambdaCollection<String>() }])

    <!OVERLOAD_RESOLUTION_AMBIGUITY!>f2<!>([{ }])
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>f2<!>([{ 42 }])
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>f2<!>([{ null!! }])
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>f2<!>([{ 42 }, { "42" }])

    f2(c2 = [{ <!RETURN_TYPE_MISMATCH!>42<!> }])
    f2(c2 = [{ "42" }])
    f2(c2 = [{ null!! }])

    <!OVERLOAD_RESOLUTION_AMBIGUITY!>f3<!>([{}])
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>f3<!>([{ 42 }])
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>f3<!>([{ null!! }])
}

/* GENERATED_FIR_TAGS: checkNotNullCall, classDeclaration, companionObject, functionDeclaration, integerLiteral,
lambdaLiteral, nullableType, objectDeclaration, operator, stringLiteral, typeParameter, vararg */
