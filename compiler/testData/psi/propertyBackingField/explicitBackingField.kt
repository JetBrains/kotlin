val simpleNumber: Number
    field = 4

val numberWithPrivateField: Number
    private field = 4

val numberWithLateinitField: Number
    lateinit field: Int

val numberWithInternalLateinitField: Number
    internal lateinit field: Int

var numberWithFieldAndAccessors: Number
    field = "test"
    get() = field.length
    set(value) {
        field = value.toString()
    }

val numberWithExplicitType: Number
    field: Int = 10

val numberWithBlockInitializer: Number
    field {
        return 10
    }

val minimalNumber
    field
