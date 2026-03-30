// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER

fun Text(text: String, color: String) {}

fun Text(value: Int, color: String) {}

val textString: (text: String, color: String) -> Unit = ::Text

fun Wrapper(...textString.$props) {
    text.length
    color.length
}

/* GENERATED_FIR_TAGS: callableReference, functionDeclaration, functionalType, propertyDeclaration */
