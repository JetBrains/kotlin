// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -FullValueClasses
// WITH_STDLIB

fun local() {
    <!VALUE_CLASS_NOT_TOP_LEVEL, VALUE_CLASS_WITHOUT_JVM_INLINE_ANNOTATION, WRONG_MODIFIER_TARGET!>value<!> class Local(val x: Int)

    @JvmInline
    <!VALUE_CLASS_NOT_TOP_LEVEL, WRONG_MODIFIER_TARGET!>value<!> class LocalInline(val x: Int)
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, localClass, primaryConstructor, propertyDeclaration, value */
