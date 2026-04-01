// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CollectionLiterals
// WITH_STDLIB

interface X
interface Y
object Z : X, Y

class LambdaCollection<T> {
    companion object {
        operator fun <T> of(vararg block: () -> LambdaCollection<T>) = LambdaCollection<T>()
    }
}

fun a(): LambdaCollection<Int> = []

fun X.b(): LambdaCollection<String> = []
fun Y.b(): LambdaCollection<Int> = []

fun X.c(): LambdaCollection<String> = []
fun Y.c(): LambdaCollection<Y> = []

fun X.d(): Int = 0
fun Y.d(): Long = 0

fun X.e(): Int = 0
fun Y.e(): String = ""

fun f(): String = ""

fun f1(x: LambdaCollection<Int>) { }
fun f1(x: LambdaCollection<String>) { }

fun f2(x: List<() -> Int>) { }
fun f2(x: List<() -> String>) { }

fun <T : () -> Int> f3(x: List<T>) { }
fun <T : () -> String> f3(x: List<T>) { }

fun test() {
    f1([::a])
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>f1<!>(<!CANNOT_INFER_PARAMETER_TYPE!>[Z::<!OVERLOAD_RESOLUTION_AMBIGUITY!>b<!>]<!>)
    f1([Z::c])

    f2([::f])
    f2([Z::d])
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>f2<!>(<!CANNOT_INFER_PARAMETER_TYPE!>[Z::<!OVERLOAD_RESOLUTION_AMBIGUITY!>e<!>]<!>)

    f3([::f])
    f3([Z::d])
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>f3<!>(<!CANNOT_INFER_PARAMETER_TYPE!>[Z::<!OVERLOAD_RESOLUTION_AMBIGUITY!>e<!>]<!>)
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, companionObject, functionDeclaration, nullableType,
objectDeclaration, operator, typeParameter, vararg */
