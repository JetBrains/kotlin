// WITH_STDLIB
val initializerAndGetter = 1
    get() = field

val initializerAndConstantGetter = 1
    get() = 2

val mismatchedInitializerAndGetter = "Hello"
    get() = 1

var initializerAndAccessors = 1
    get() = field
    set(value) { field = value }

val getterWithBlockBody = 1
    get() {
        return field
    }

val initializerAndGetterWithExplicitType = 1
    get(): Int = field
