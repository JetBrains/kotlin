// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-89374
// LANGUAGE: +CompanionBlocks
// WITH_STDLIB

@JvmInline
value class Foo(val x: Int) {
    companion {
        <!PROPERTY_WITH_BACKING_FIELD_INSIDE_VALUE_CLASS!>val a0<!> = 0

        <!PROPERTY_WITH_BACKING_FIELD_INSIDE_VALUE_CLASS!>var a1: Int<!> = 0
            get() = 1
            set(value) {
                field = value
            }

        const <!PROPERTY_WITH_BACKING_FIELD_INSIDE_VALUE_CLASS!>val a2<!> = 0

        @JvmField
        <!PROPERTY_WITH_BACKING_FIELD_INSIDE_VALUE_CLASS!>val a3<!> = 0

        lateinit <!PROPERTY_WITH_BACKING_FIELD_INSIDE_VALUE_CLASS!>var a4: String<!>

        val a5 by <!DELEGATED_PROPERTY_INSIDE_VALUE_CLASS!>lazy { 0 }<!>

        val a6: Int
            get() = 0
    }

    <!PROPERTY_WITH_BACKING_FIELD_INSIDE_VALUE_CLASS!>val b0<!> = 0

    val b1 by <!DELEGATED_PROPERTY_INSIDE_VALUE_CLASS!>lazy { 0 }<!>

    val b2: Int
        get() = 0
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, const, getter, integerLiteral, lambdaLiteral, lateinit,
nullableType, primaryConstructor, propertyDeclaration, propertyDelegate, setter, value */
