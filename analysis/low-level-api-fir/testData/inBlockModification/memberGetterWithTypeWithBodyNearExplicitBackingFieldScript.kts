// LANGUAGE: +ExplicitBackingFields

class A {
    val x: Number
        field: Int = 1
        get(): Number {
            val i = field
            return <expr>i</expr>
        }
}
