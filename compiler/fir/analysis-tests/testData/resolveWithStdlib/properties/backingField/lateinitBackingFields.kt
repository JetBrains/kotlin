var that: Int
    lateinit field: String
    get() = field.length
    set(value) {
        field = value.toString()
    }

fun test() {
    that = 1
    println(that)
}

// Not allowed for properties with
// custom accessors
<!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var number: Number
    field = 4
    set(value) {
        field = 10
    }

val something: Number
    <!LATEINIT_FIELD_IN_VAL_PROPERTY, LATEINIT_PROPERTY_FIELD_DECLARATION_WITH_INITIALIZER!>lateinit<!> field = 4

<!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var oneMore: Number
    <!LATEINIT_PROPERTY_FIELD_DECLARATION_WITH_INITIALIZER!>lateinit<!> field = 4
    set(value) {
        field = 10
    }

var thingWithNullableField: Number
    <!LATEINIT_NULLABLE_BACKING_FIELD!>lateinit<!> field: String?
    get() = 20
    set(value) {
        field = value.toString()
    }
