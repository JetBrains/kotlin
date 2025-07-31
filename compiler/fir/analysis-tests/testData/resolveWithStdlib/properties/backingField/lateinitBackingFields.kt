// RUN_PIPELINE_TILL: FRONTEND
<!INCONSISTENT_BACKING_FIELD_TYPE!>var that: Int<!>
    <!WRONG_MODIFIER_TARGET!>lateinit<!> field: String
    <!PROPERTY_WITH_EXPLICIT_FIELD_AND_ACCESSORS!>get()<!> = field.length
    <!PROPERTY_WITH_EXPLICIT_FIELD_AND_ACCESSORS!>set(value)<!> {
        field = value.toString()
    }

fun test() {
    that = 1
    println(that)
}

// Not allowed for properties with
// custom accessors & backing fields
<!INAPPLICABLE_LATEINIT_MODIFIER, INAPPLICABLE_LATEINIT_MODIFIER, UNNECESSARY_LATEINIT!>lateinit<!> var number: Number
    field = 4
    <!PROPERTY_WITH_EXPLICIT_FIELD_AND_ACCESSORS!>set(value)<!> {
        field = 10
    }

val something: Number
    <!LATEINIT_FIELD_IN_VAL_PROPERTY, LATEINIT_PROPERTY_FIELD_DECLARATION_WITH_INITIALIZER, WRONG_MODIFIER_TARGET!>lateinit<!> field = 4

<!INAPPLICABLE_LATEINIT_MODIFIER, INAPPLICABLE_LATEINIT_MODIFIER, UNNECESSARY_LATEINIT!>lateinit<!> var oneMore: Number
    <!LATEINIT_PROPERTY_FIELD_DECLARATION_WITH_INITIALIZER, WRONG_MODIFIER_TARGET!>lateinit<!> field = 4
    <!PROPERTY_WITH_EXPLICIT_FIELD_AND_ACCESSORS!>set(value)<!> {
        field = 10
    }

<!INCONSISTENT_BACKING_FIELD_TYPE!>var thingWithNullableField: Number<!>
    <!LATEINIT_NULLABLE_BACKING_FIELD, WRONG_MODIFIER_TARGET!>lateinit<!> field: String?
    <!PROPERTY_WITH_EXPLICIT_FIELD_AND_ACCESSORS!>get()<!> = 20
    <!PROPERTY_WITH_EXPLICIT_FIELD_AND_ACCESSORS!>set(value)<!> {
        field = value.toString()
    }

/* GENERATED_FIR_TAGS: assignment, functionDeclaration, getter, integerLiteral, lateinit, nullableType,
propertyDeclaration, setter */
