// RUN_PIPELINE_TILL: BACKEND

fun <T> a(): T = TODO()
val b: Unit = throw a()

/* GENERATED_FIR_TAGS: functionDeclaration, nullableType, propertyDeclaration, typeParameter */
