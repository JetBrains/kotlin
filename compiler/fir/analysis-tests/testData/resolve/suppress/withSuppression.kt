const val x = "123"
    @Suppress("CONST_VAL_WITH_GETTER")
    get() = field

val y = "789"

const val z = @Suppress("CONST_VAL_WITH_NON_CONST_INITIALIZER") y

@Target(AnnotationTarget.TYPE)
annotation class Ann

fun foo(): @Suppress("REPEATED_ANNOTATION") @Ann @Ann Int = 42

typealias Alias<T> = @Suppress("TYPEALIAS_SHOULD_EXPAND_TO_CLASS") T

interface A

interface B : @Suppress("SUPERTYPE_INITIALIZED_IN_INTERFACE") A<!NO_CONSTRUCTOR!>()<!>

data class D @Suppress("DATA_CLASS_VARARG_PARAMETER") constructor(vararg val x: String)
