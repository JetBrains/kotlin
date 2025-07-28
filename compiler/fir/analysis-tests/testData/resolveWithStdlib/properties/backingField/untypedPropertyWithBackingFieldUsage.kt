// RUN_PIPELINE_TILL: FRONTEND
<!PROPERTY_WITH_NO_TYPE_NO_INITIALIZER!>val simpleNoTypeBlock<!>
    get() {
        return <!UNRESOLVED_REFERENCE!>field<!>
    }

val simpleNoTypeExpression
    get() = <!UNRESOLVED_REFERENCE!>field<!>

<!MUST_BE_INITIALIZED!>val simpleTypeBlock: Int<!>
    get() {
        return field
    }

<!MUST_BE_INITIALIZED!>val simpleTypeExpression: Int<!>
    get() = field

<!INCONSISTENT_BACKING_FIELD_TYPE!>val withFieldNoTypeBlock<!>
    field = 3.14
    get() {
        // *TODO: add support for this?
        return <!UNRESOLVED_REFERENCE!>field<!>.toInt()
    }

<!INCONSISTENT_BACKING_FIELD_TYPE!>val withFieldNoTypeExpression<!>
    field = 3.14
    get() = <!UNRESOLVED_REFERENCE!>field<!>.toInt()

<!INCONSISTENT_BACKING_FIELD_TYPE!>val withFieldTypeBlock: Int<!>
    field = 3.14
    get() {
        return field.toInt()
    }

<!INCONSISTENT_BACKING_FIELD_TYPE!>val withFieldTypeExpression: Int<!>
    field = 3.14
    get() = field.toInt()

// If * is supported, this is a relevant message
// since adding a getter _may_ be enough
<!INCONSISTENT_BACKING_FIELD_TYPE!>val minimalFieldWithInitializer<!>
    field = 1

<!INCONSISTENT_BACKING_FIELD_TYPE!>val minimalFieldWithNoInitializer<!>
    <!PROPERTY_FIELD_DECLARATION_MISSING_INITIALIZER!>field: Int<!>

// TODO: redundant backing field?
// Or we assume someone may still want
// to access it directly via `myProperty#field`?
<!INCONSISTENT_BACKING_FIELD_TYPE!>val constWithFieldNoTypeBlock<!>
    field = 3.14
    get() = 10

<!INCONSISTENT_BACKING_FIELD_TYPE!>val constWithFieldNoTypeExpression<!>
    field = 3.14
    get() = 10

<!INCONSISTENT_BACKING_FIELD_TYPE!>val constWithFieldTypeBlock: Int<!>
    field = 3.14
    get() = 10

<!INCONSISTENT_BACKING_FIELD_TYPE!>val constWithFieldTypeExpression: Int<!>
    field = 3.14
    get() = 10

/* GENERATED_FIR_TAGS: getter, integerLiteral, propertyDeclaration */
