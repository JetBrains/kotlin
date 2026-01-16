// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-83713
// FIR_DUMP

class NotExternal(external val paramProp: String, <!WRONG_MODIFIER_TARGET!>external<!> param: String) {
    <!WRONG_MODIFIER_TARGET!>external<!> val prop: String

    external fun foo()

    <!WRONG_MODIFIER_TARGET!>external<!> constructor(): this("", "")
}

/* GENERATED_FIR_TAGS: classDeclaration, external, functionDeclaration, primaryConstructor, propertyDeclaration,
secondaryConstructor, stringLiteral */
