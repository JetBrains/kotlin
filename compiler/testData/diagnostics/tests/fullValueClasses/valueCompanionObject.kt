// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +FullValueClasses
// WITH_STDLIB

open class Identity

class Outer {
    <!WRONG_MODIFIER_TARGET!>value<!> companion object : <!VALUE_CLASS_CANNOT_EXTEND_IDENTITY_CLASSES!>Identity<!>() {
        <!PROPERTY_WITH_BACKING_FIELD_INSIDE_VALUE_CLASS!>val z<!> = 1
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, integerLiteral, objectDeclaration, propertyDeclaration, value */
