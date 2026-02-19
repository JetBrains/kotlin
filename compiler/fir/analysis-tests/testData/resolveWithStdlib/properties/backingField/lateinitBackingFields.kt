// RUN_PIPELINE_TILL: FRONTEND
<!INCONSISTENT_BACKING_FIELD_TYPE!><!VAR_PROPERTY_WITH_EXPLICIT_BACKING_FIELD!>var<!> that: Int<!>
    <!WRONG_MODIFIER_TARGET!>lateinit<!> field: String
    <!PROPERTY_WITH_EXPLICIT_FIELD_AND_ACCESSORS!>get()<!> = field.length
    set(value) {
        field = value.toString()
    }

fun test() {
    that = 1
    println(that)
}

// Not allowed for properties with
// custom accessors & backing fields
<!INAPPLICABLE_LATEINIT_MODIFIER, INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> <!VAR_PROPERTY_WITH_EXPLICIT_BACKING_FIELD!>var<!> number: Number
    field = 4
    set(value) {
        field = 10
    }

val something: Number
    <!WRONG_MODIFIER_TARGET!>lateinit<!> field = 4

<!INAPPLICABLE_LATEINIT_MODIFIER, INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> <!VAR_PROPERTY_WITH_EXPLICIT_BACKING_FIELD!>var<!> oneMore: Number
    <!WRONG_MODIFIER_TARGET!>lateinit<!> field = 4
    set(value) {
        field = 10
    }

<!INCONSISTENT_BACKING_FIELD_TYPE!><!VAR_PROPERTY_WITH_EXPLICIT_BACKING_FIELD!>var<!> thingWithNullableField: Number<!>
    <!LATEINIT_NULLABLE_BACKING_FIELD, WRONG_MODIFIER_TARGET!>lateinit<!> field: String?
    <!PROPERTY_WITH_EXPLICIT_FIELD_AND_ACCESSORS!>get()<!> = 20
    set(value) {
        field = value.toString()
    }

/* GENERATED_FIR_TAGS: assignment, explicitBackingField, functionDeclaration, getter, integerLiteral, lateinit,
nullableType, propertyDeclaration, setter */
