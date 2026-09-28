// RUN_PIPELINE_TILL: CODEGEN
val <T: Any> T.self: T get() = this

/* GENERATED_FIR_TAGS: getter, propertyDeclaration, propertyWithExtensionReceiver, thisExpression, typeConstraint,
typeParameter */
