// RUN_PIPELINE_TILL: FRONTEND
// OPT_IN: kotlin.js.ExperimentalWasmJsInterop

// FILE: composable.kt
package androidx.compose.runtime

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.CLASS, AnnotationTarget.PROPERTY_GETTER)
annotation class Composable

// FILE: test.kt
import androidx.compose.runtime.Composable

@Composable
fun composableFun(): Int = <!JSCODE_UNSUPPORTED_FUNCTION_KIND!>js<!>("1")
