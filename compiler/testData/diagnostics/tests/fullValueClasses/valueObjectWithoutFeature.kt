// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -FullValueClasses
// WITH_STDLIB

// Without the FullValueClasses feature a value object is not allowed at all (WRONG_MODIFIER_TARGET), and the value
// class declaration checker must behave as before, i.e. must not run on the object. So none of the value-class
// declaration diagnostics (extend-identity, cloneable, backing-field, @JvmInline, ...) are reported here.

open class IdentityClass

<!WRONG_MODIFIER_TARGET!>value<!> object Foo

class Outer {
    <!WRONG_MODIFIER_TARGET!>value<!> object Nested
}

<!WRONG_MODIFIER_TARGET!>value<!> object ExtendsIdentity : IdentityClass()

<!WRONG_MODIFIER_TARGET!>value<!> object CloneableObject : Cloneable

<!WRONG_MODIFIER_TARGET!>value<!> object WithBackingField {
    val x: Int = 42
}

@JvmInline
<!WRONG_MODIFIER_TARGET!>value<!> object JvmInlineObject

/* GENERATED_FIR_TAGS: classDeclaration, integerLiteral, nestedClass, objectDeclaration, propertyDeclaration, value */
