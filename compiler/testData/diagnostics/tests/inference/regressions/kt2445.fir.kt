// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNREACHABLE_CODE
//KT-2445 Calling method with function with generic parameter causes compile-time exception
package a

fun main() {
    <!CANNOT_INFER_PARAMETER_TYPE!>test<!> <!CANNOT_INFER_PARAMETER_TYPE!>{

    }<!>
}

fun <R> test(callback: (R) -> Unit):Unit = callback(null!!)
