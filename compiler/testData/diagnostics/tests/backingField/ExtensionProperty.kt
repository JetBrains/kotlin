// See KT-9303: synthetic field variable does not exist for extension properties
val String.foo: Int
    get() {
        // No shadowing here
        val field = 42
        return field
    }

val String.bar: Int = <!PROPERTY_INITIALIZER_NO_BACKING_FIELD!>13<!>
    // Error
    get() = <!UNRESOLVED_REFERENCE!>field<!>

class My {
    val String.x: Int = <!PROPERTY_INITIALIZER_NO_BACKING_FIELD!>7<!>
        // Error
        get() = <!UNRESOLVED_REFERENCE!>field<!>
}