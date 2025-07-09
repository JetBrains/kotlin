// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CheckOptInOnPureEnumEntries
@RequiresOptIn
annotation class O

enum class Enum1 @O constructor() {
    ENTRY<!OPT_IN_USAGE_ERROR!><!>(),
    <!OPT_IN_USAGE_ERROR!>ENTRY2;<!>
}

enum class Enum2 {
    ENTRY<!OPT_IN_USAGE_ERROR!><!>(),
    <!OPT_IN_USAGE_ERROR!>ENTRY2,<!>
    ENTRY3<!OPT_IN_USAGE_ERROR!><!>(0);

    val x: Int

    @O constructor(x: Int = 0) {
        this.x = x
    }
}

/* GENERATED_FIR_TAGS: annotationDeclaration, assignment, enumDeclaration, enumEntry, integerLiteral, primaryConstructor,
propertyDeclaration, secondaryConstructor, thisExpression */
