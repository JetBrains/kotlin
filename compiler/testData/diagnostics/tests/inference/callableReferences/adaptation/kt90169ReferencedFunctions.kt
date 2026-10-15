// RUN_PIPELINE_TILL: CODEGEN
// ISSUE: KT-90169
// WITH_STDLIB
// FULL_JDK

fun <T> applyIt(t: T, f: (T) -> Int): Int = f(t)
fun <T, R> applyR(t: T, f: (T) -> R): R = f(t)

inline fun <reified E> countR(vararg xs: E): Int = xs.size
fun <E : Any> countNN(vararg xs: E): Int = xs.size
fun <E> countD(vararg xs: E, k: Int = 0): Int = xs.size + k
fun <E> firstG(vararg xs: E): E = xs[0]

class K { fun <E> countM(vararg xs: E): Int = xs.size }
object O { fun <E> countM(vararg xs: E): Int = xs.size }
class Box<E>(vararg val xs: E)

fun main(arr: Array<String>) {
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.List<kotlin.String>")!>arr.let(::listOf)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Set<kotlin.String>")!>arr.let(::setOf)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.MutableList<kotlin.String>")!>arr.let(::mutableListOf)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("java.util.ArrayList<kotlin.String>")!>arr.let(::arrayListOf)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.MutableSet<kotlin.String>")!>arr.let(::mutableSetOf)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("java.util.HashSet<kotlin.String>")!>arr.let(::hashSetOf)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.sequences.Sequence<kotlin.String>")!>arr.let(::sequenceOf)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.List<kotlin.String>")!>arr.let(::listOfNotNull)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Array<kotlin.String>")!>arr.let(::arrayOf)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("(kotlin.collections.MutableList<(kotlin.String..kotlin.String?)>..kotlin.collections.List<(kotlin.String..kotlin.String?)>?)")!>arr.let(java.util.Arrays::asList)<!>

    applyIt(arr, <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction1<@ParameterName(...) kotlin.Array<out kotlin.String>, kotlin.Int>")!>::countR<!>)
    applyIt(arr, <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction1<@ParameterName(...) kotlin.Array<out kotlin.String>, kotlin.Int>")!>K()::countM<!>)
    applyIt(arr, <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction1<@ParameterName(...) kotlin.Array<out kotlin.String>, kotlin.Int>")!>O::countM<!>)
    <!DEBUG_INFO_EXPRESSION_TYPE("Box<kotlin.String>")!>applyR(arr, ::Box)<!>
    applyIt(arr, <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction1<@ParameterName(...) kotlin.Array<out kotlin.String>, kotlin.Int>")!>::countNN<!>)
    applyIt(arr, <!DEBUG_INFO_EXPRESSION_TYPE("(kotlin.Array<out kotlin.String>) -> kotlin.Int")!>::countD<!>)
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>arr.let(::firstG)<!>
}

/* GENERATED_FIR_TAGS: additiveExpression, callableReference, capturedType, classDeclaration, flexibleType,
functionDeclaration, functionalType, inline, integerLiteral, javaCallableReference, nullableType, objectDeclaration,
outProjection, primaryConstructor, propertyDeclaration, reified, typeConstraint, typeParameter, vararg */
