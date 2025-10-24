// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-39697
// ISSUE: KT-81918
import java.io.Serializable

interface In<in U>

fun <T0> myMap0(x: (T0) -> Unit): T0 = TODO()
fun <T1> myMap1(v: T1, x: (T1) -> Unit): T1 = TODO()
fun <T2> myMap2(v: In<T2>, x: (T2) -> Unit): T2 = TODO()

fun ofString(vararg args: String) {}
fun ofAny(vararg args: Any) {}
fun ofInt(vararg args: Int) {}

fun main(
    myString: String,
    myAny: Any,
    myInt: Int,
    myArrayAny: Array<Any>,
    myArrayOutAny: Array<out Any>,
    myArrayString: Array<String>,
    myArrayInt: Array<Int>,
    myIntArray: IntArray,
    myInString: In<String>,
    myInAny: In<Any>,
    myInInt: In<Int>,
    mySerializable: java.io.Serializable,
    myInArrayAny: In<Array<Any>>,
    myInArrayOutAny: In<Array<out Any>>,
    myInArrayString: In<Array<String>>,
    myInArrayInt: In<Array<Int>>,
    myInIntArray: In<IntArray>,
    myInSerializable: In<java.io.Serializable>,
) {
    <!DEBUG_INFO_EXPRESSION_TYPE("@ParameterName(...) kotlin.Array<out kotlin.String>")!>myMap0(::ofString)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("@ParameterName(...) kotlin.Array<out kotlin.Any>")!>myMap0(::ofAny)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("@ParameterName(...) kotlin.IntArray")!>myMap0(::ofInt)<!>

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Array<kotlin.String>")!>myMap0<Array<String>>(::ofString)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Array<kotlin.Any>")!>myMap0<Array<Any>>(::ofAny)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Array<out kotlin.Any>")!>myMap0<Array<out Any>>(::ofAny)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Array<kotlin.Int>")!>myMap0<Array<Int>>(::<!INAPPLICABLE_CANDIDATE!>ofInt<!>)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.IntArray")!>myMap0<IntArray>(::ofInt)<!>

    <!DEBUG_INFO_EXPRESSION_TYPE("java.io.Serializable")!>myMap0<Serializable>(::<!INAPPLICABLE_CANDIDATE!>ofAny<!>)<!>

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>myMap0<String>(::<!INAPPLICABLE_CANDIDATE!>ofString<!>)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>myMap0<Any>(::<!INAPPLICABLE_CANDIDATE!>ofAny<!>)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>myMap0<Int>(::<!INAPPLICABLE_CANDIDATE!>ofInt<!>)<!>

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>myMap1(myString, ::<!INAPPLICABLE_CANDIDATE!>ofString<!>)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>myMap1(myAny, ::<!INAPPLICABLE_CANDIDATE!>ofAny<!>)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>myMap1(myInt, ::<!INAPPLICABLE_CANDIDATE!>ofInt<!>)<!>

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Array<kotlin.String>")!>myMap1(myArrayString, ::ofString)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Array<kotlin.Any>")!>myMap1(myArrayAny, ::ofAny)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Array<out kotlin.Any>")!>myMap1(myArrayOutAny, ::ofAny)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Array<kotlin.Int>")!>myMap1(myArrayInt, ::<!INAPPLICABLE_CANDIDATE!>ofInt<!>)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.IntArray")!>myMap1(myIntArray, ::ofInt)<!>

    <!DEBUG_INFO_EXPRESSION_TYPE("java.io.Serializable")!>myMap1(mySerializable, ::<!INAPPLICABLE_CANDIDATE!>ofAny<!>)<!>

    // TODO: The behavior should change for the cases with INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION_WARNING after KT-81918
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>myMap2(myInString, ::<!INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION_WARNING!>ofString<!>)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("@ParameterName(...) kotlin.Array<out kotlin.Any>")!>myMap2(myInAny, ::ofAny)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>myMap2(myInInt, ::<!INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION_WARNING!>ofInt<!>)<!>

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Array<kotlin.String>")!>myMap2(myInArrayString, ::ofString)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Array<kotlin.Any>")!>myMap2(myInArrayAny, ::ofAny)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("@ParameterName(...) kotlin.Array<out kotlin.Any>")!>myMap2(myInArrayOutAny, ::ofAny)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Array<kotlin.Int>")!>myMap2(myInArrayInt, ::<!INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION_WARNING!>ofInt<!>)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("@ParameterName(...) kotlin.IntArray")!>myMap2(myInIntArray, ::ofInt)<!>

    <!DEBUG_INFO_EXPRESSION_TYPE("@ParameterName(...) kotlin.Array<out kotlin.Any>")!>myMap2(myInSerializable, ::ofAny)<!>
}

/* GENERATED_FIR_TAGS: callableReference, functionDeclaration, functionalType, in, interfaceDeclaration, nullableType,
outProjection, typeParameter, vararg */
