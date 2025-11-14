// RUN_PIPELINE_TILL: BACKEND
// RENDER_DIAGNOSTICS_FULL_TEXT

enum class Color {BLUE, GREEN}

fun describeColor(c: Color) : String {
    return when(c) {
        Color.GREEN -> "Green"
        Color.BLUE -> "Blue"
    }
}

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry, equalityExpression, functionDeclaration, smartcast, stringLiteral,
whenExpression, whenWithSubject */
