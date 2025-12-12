// RUN_PIPELINE_TILL: FRONTEND
// FULL_JDK
// WITH_STDLIB

import java.util.function.Supplier
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KProperty0

// See com.intellij.codeInspection.options.OptionController.onValue
fun onValue1(x: Supplier<Any>): Int = 1
fun onValue1(x: KMutableProperty0<*>): String = ""

fun onValue2(x: Supplier<Any>): Int = 1
fun onValue2(x: KProperty0<*>): String = ""

var mutableVar: String = ""

fun main() {
    val x1 = <!OVERLOAD_RESOLUTION_AMBIGUITY!>onValue1<!>(::mutableVar)
    val x2 = <!OVERLOAD_RESOLUTION_AMBIGUITY!>onValue2<!>(::mutableVar)

    <!DEBUG_INFO_EXPRESSION_TYPE("ERROR CLASS: Ambiguity: onValue1, [/onValue1, /onValue1]")!>x1<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("ERROR CLASS: Ambiguity: onValue2, [/onValue2, /onValue2]")!>x2<!>
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, lambdaLiteral, localProperty, nullableType,
propertyDeclaration, samConversion, stringLiteral, typeParameter */
