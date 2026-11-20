// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-90169

// FILE: J.java
public class J {
    public static <T> int countJ(T... xs) { return xs.length; }
}

// FILE: main.kt
fun <T1> myMap1(v: T1, x: (T1) -> Int): T1 = TODO()

fun <E> countG(vararg xs: E): Int = xs.size
fun <N : Number> countN(vararg xs: N): Int = xs.size
fun count(vararg xs: Any?): Int = xs.size

fun main(
    myString: String,
    myArrayString: Array<String>,
    myArrayInt: Array<Int>,
    myIntArray: IntArray,
) {
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Array<kotlin.String>")!>myMap1(myArrayString, <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction1<@ParameterName(...) kotlin.Array<out kotlin.String>, kotlin.Int>")!>::countG<!>)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Array<kotlin.String>")!>myMap1(myArrayString, <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction1<@ParameterName(...) kotlin.Array<out kotlin.Any?>, kotlin.Int>")!>::count<!>)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Array<kotlin.Int>")!>myMap1(myArrayInt, <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction1<@ParameterName(...) kotlin.Array<out kotlin.Int>, kotlin.Int>")!>::countG<!>)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.IntArray")!>myMap1(myIntArray, <!DEBUG_INFO_EXPRESSION_TYPE("(kotlin.IntArray) -> kotlin.Int")!>::countG<!>)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>myMap1(myString, <!DEBUG_INFO_EXPRESSION_TYPE("(kotlin.String) -> kotlin.Int")!>::countG<!>)<!>

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Array<kotlin.String>")!>myMap1<Array<String>>(myArrayString, <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction1<@ParameterName(...) kotlin.Array<out kotlin.String>, kotlin.Int>")!>::countG<!>)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>myMap1<String>(myString, <!DEBUG_INFO_EXPRESSION_TYPE("(kotlin.String) -> kotlin.Int")!>::countG<!>)<!>

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Array<kotlin.Int>")!>myMap1(myArrayInt, <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction1<@ParameterName(...) kotlin.Array<out kotlin.Int>, kotlin.Int>")!>::countN<!>)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Array<kotlin.String>")!>myMap1(myArrayString, <!DEBUG_INFO_EXPRESSION_TYPE("ERROR CLASS: Inapplicable(INAPPLICABLE): /countN")!>::<!INAPPLICABLE_CANDIDATE!>countN<!><!>)<!>

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Array<kotlin.String>")!>myMap1(myArrayString, <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction1<(@ParameterName(...) kotlin.Array<(kotlin.String..kotlin.String?)>..@ParameterName(...) kotlin.Array<out (kotlin.String..kotlin.String?)>?), kotlin.Int>")!>J::countJ<!>)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>myMap1(myString, <!DEBUG_INFO_EXPRESSION_TYPE("((kotlin.String..kotlin.String?)) -> kotlin.Int")!>J::countJ<!>)<!>
}

/* GENERATED_FIR_TAGS: callableReference, flexibleType, functionDeclaration, functionalType, javaCallableReference,
nullableType, outProjection, typeConstraint, typeParameter, vararg */
