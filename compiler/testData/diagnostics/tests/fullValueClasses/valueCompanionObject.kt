// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +FullValueClasses
// WITH_STDLIB

open class Identity

class Outer {
    <!WRONG_MODIFIER_TARGET!>value<!> companion object : Identity() {
        val z = 1
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, integerLiteral, objectDeclaration, propertyDeclaration, value */
