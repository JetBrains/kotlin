// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
class List<out T>(val size : Int) {
    companion object {
        val Nil = List<Nothing>(0)
    }
}

fun List<String>.join() =
        when (this) {
            List.Nil -> "[]" // CANNOT_CHECK_FOR_ERASED was reported
            else -> ""
        }

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, equalityExpression, funWithExtensionReceiver,
functionDeclaration, integerLiteral, nullableType, objectDeclaration, out, primaryConstructor, propertyDeclaration,
stringLiteral, typeParameter, whenExpression, whenWithSubject */
