// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-76663
// FIR_DUMP
// DUMP_INFERENCE_LOGS: FIXATION, MARKDOWN, MERMAID

class Test(val something: String?) {
    fun toPlainObj(data: String): <!UNSUPPORTED!>dynamic<!> {
        return if (something == null) {
            throw IllegalStateException("Something is not defined.")
        } else {
            <!UNSUPPORTED("Dynamic type is only supported in Kotlin JS.")!>parse<!>(data)
        }
    }
}

fun <T> parse(data: String): T = TODO()

/* GENERATED_FIR_TAGS: classDeclaration, equalityExpression, flexibleType, functionDeclaration, ifExpression,
nullableType, primaryConstructor, propertyDeclaration, stringLiteral, typeParameter */
