// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +FullValueClasses
// RENDER_DIAGNOSTICS_FULL_TEXT
// ISSUE: KT-91600

fun f(): Any {
    <!VALUE_CLASS_NOT_TOP_LEVEL, WRONG_MODIFIER_TARGET!>value<!> class Loc(val x: Int, val y: Int)
    return Loc(1, 2)
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, localClass, primaryConstructor,
propertyDeclaration, value */
