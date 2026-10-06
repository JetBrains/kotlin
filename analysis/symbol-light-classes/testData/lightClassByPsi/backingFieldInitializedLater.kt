package test

@Target(AnnotationTarget.FIELD)
annotation class FieldAnn

class InitBlock {
    val foo: Int
        get() {
            return field
        }

    init {
        foo = 4
    }

    val bar: Int
        get() {
            val field = 42
            return field
        }
}

class SecondaryConstructor {
    val foo: Int
        get() = field

    constructor(i: Int) {
        foo = i
    }
}

class AnnotatedField {
    @field:FieldAnn
    val foo: Int
        get() = field

    init {
        foo = 1
    }
}
