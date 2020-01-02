// See KT-9303: synthetic field variable does not exist for extension properties
val String.foo: Int
    get() {
        // No shadowing here
        val field = 42
        return field
    }

val String.bar: Int = 13
    // Error
    get() = field

class My {
    val String.x: Int = 7
        // Error
        get() = field
}