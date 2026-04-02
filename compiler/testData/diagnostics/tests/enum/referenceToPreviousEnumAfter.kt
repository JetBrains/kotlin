// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +FixedUninitializedEnumCompanionCheck

enum class E(val higherPriority: E?) {
    Foo(null),
    Bar(Foo),
    Baz(E.Foo),
    Qux(<!UNINITIALIZED_ENUM_COMPANION!>companionProp<!>),
    Quux(<!UNINITIALIZED_ENUM_COMPANION!>E<!>.companionProp),
    ;

    companion object {
        val companionProp: E? = null
    }
}

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry, nullableType, primaryConstructor, propertyDeclaration */
