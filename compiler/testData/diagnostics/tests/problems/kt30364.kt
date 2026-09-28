// RUN_PIPELINE_TILL: CODEGEN
// ISSUE: KT-30364

// KT-30364: Qualified `this` expression is not resolved inside anonymous object in an extension property
val String.prop
    get() = object {
        fun get(): String {
            return this@prop
        }
    }

/* GENERATED_FIR_TAGS: anonymousObjectExpression, functionDeclaration, getter, propertyDeclaration,
propertyWithExtensionReceiver, thisExpression */
