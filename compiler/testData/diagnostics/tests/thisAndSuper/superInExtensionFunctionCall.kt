// RUN_PIPELINE_TILL: FRONTEND
// No supertype at all

fun Any.extension(arg: Any?) {}

class A1 {
    fun test() {
        <!SUPER_CANT_BE_EXTENSION_RECEIVER!>super<!>.extension(null) // Call to an extension function
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, nullableType, superExpression */
