// RUN_PIPELINE_TILL: BACKEND
fun <K> materialize(): K? { return null }

val x: String? by lazy { materialize() }

/* GENERATED_FIR_TAGS: functionDeclaration, lambdaLiteral, nullableType, propertyDeclaration, propertyDelegate,
typeParameter */
