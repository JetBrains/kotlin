// LANGUAGE: +ExplicitBackingFields

class Foo {
    val member: Int = 1

    val x: Number
        field: Int = run {
            <expr>member</expr>
        }
}
