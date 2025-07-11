// RUN_PIPELINE_TILL: FRONTEND
// FIR_DUMP
// LANGUAGE: -CheckOptInOnPureEnumEntries
@RequiresOptIn
annotation class O

enum class Enum1 @O constructor() {
    ENTRY<!OPT_IN_USAGE_ERROR!><!>(),
    ENTRY2,
    @OptIn(O::class) ENTRY3;
}

enum class Enum2 {
    ENTRY<!OPT_IN_USAGE_ERROR!><!>(),
    ENTRY2,
    ENTRY3<!OPT_IN_USAGE_ERROR!><!>(0);

    val x: Int

    @O constructor(x: Int = 0) {
        this.x = x
    }
}

enum class Enum3 @O constructor(x: Int = 42) {
    ENTRY(),
    <!ENUM_ENTRY_SHOULD_BE_INITIALIZED!>ENTRY2,<!>
    ENTRY3<!OPT_IN_USAGE_ERROR!><!>(3);

    val x: Int = x

    @OptIn(O::class)
    constructor() : this(0)
}

/* GENERATED_FIR_TAGS: annotationDeclaration, assignment, enumDeclaration, enumEntry, integerLiteral, primaryConstructor,
propertyDeclaration, secondaryConstructor, thisExpression */
