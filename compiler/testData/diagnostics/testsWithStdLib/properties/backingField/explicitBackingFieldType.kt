// RUN_PIPELINE_TILL: FRONTEND
class A(c : Inv<out Number>) {
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
    <!PROPERTY_WITH_EXPLICIT_FIELD_AND_ACCESSORS!>get()<!> = 30

    val uninitialized: Number
    <!EXPLICIT_FIELD_MUST_BE_INITIALIZED!>field: Int<!>

    val uninitializedWithGetter: Number
    <!EXPLICIT_FIELD_MUST_BE_INITIALIZED!>field: Int<!>
    <!PROPERTY_WITH_EXPLICIT_FIELD_AND_ACCESSORS!>get()<!> = 2

    val initiaizedWithExplicitBackingField = <!PROPERTY_INITIALIZER_WITH_EXPLICIT_FIELD_DECLARATION!>listOf(1, 2)<!>
    field: MutableList<Int>

    val p = 5
        get() = field

    <!INCONSISTENT_BACKING_FIELD_TYPE!><!VAR_PROPERTY_WITH_EXPLICIT_BACKING_FIELD!>var<!> setterNeeded: Int<!>
        field = "test"
        <!PROPERTY_WITH_EXPLICIT_FIELD_AND_ACCESSORS!>get()<!> = field.length

    val anonymousType: Any
        field = object {
            val x: Int = 1
        }

    val capturedType: Number?
        field = c.x

}

class Inv<T>(val x: T)

/* GENERATED_FIR_TAGS: classDeclaration, explicitBackingField, getter, integerLiteral, nullableType, propertyDeclaration,
stringLiteral */
