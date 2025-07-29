// RUN_PIPELINE_TILL: FRONTEND
class A {
    val a = 20

    val it: Number
    field = 4

    <!INCONSISTENT_BACKING_FIELD_TYPE!>val joke: Number<!>
    field = "Haha"

    <!INCONSISTENT_BACKING_FIELD_TYPE!>val incompatible: Number<!>
    field: Any? = 42

    <!INCONSISTENT_BACKING_FIELD_TYPE!>val customGetterNeeded: Int<!>
    field: Number = 42

    <!INCONSISTENT_BACKING_FIELD_TYPE!>val invertedTypes: Int<!>
    field: Number = 42
    get() = 30

    val uninitialized: Number
    <!EXPLICIT_FIELD_MUST_BE_INITIALIZED!>field: Int<!>

    val uninitializedWithGetter: Number
    <!EXPLICIT_FIELD_MUST_BE_INITIALIZED!>field: Int<!>
    get() = 2

    val initiaizedWithExplicitBackingField = <!PROPERTY_INITIALIZER_WITH_EXPLICIT_FIELD_DECLARATION!>listOf(1, 2)<!>
    field: MutableList<Int>

    val p = 5
        get() = field

    <!INCONSISTENT_BACKING_FIELD_TYPE!>var setterNeeded: Int<!>
        field = "test"
        get() = field.length
}

/* GENERATED_FIR_TAGS: classDeclaration, getter, integerLiteral, nullableType, propertyDeclaration, stringLiteral */
