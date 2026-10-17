// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -FullValueClasses
// WITH_STDLIB

fun local() {
    <!WRONG_MODIFIER_TARGET!>value<!> class Local(val x: Int)

    @JvmInline
    <!WRONG_MODIFIER_TARGET!>value<!> class LocalInline(val x: Int)
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, localClass, primaryConstructor, propertyDeclaration, value */
