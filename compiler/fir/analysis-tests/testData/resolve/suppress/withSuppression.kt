const val x = "123"
    <!CONST_VAL_WITH_GETTER!>@Suppress("CONST_VAL_WITH_GETTER")
    get() = field<!>

val y = "789"

const val z = @Suppress("CONST_VAL_WITH_NON_CONST_INITIALIZER") <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>y<!>

@Target(AnnotationTarget.TYPE)
annotation class Ann

fun foo(): @Suppress("REPEATED_ANNOTATION") @Ann <!REPEATED_ANNOTATION!>@Ann<!> Int = 42

typealias Alias<T> = <!TYPEALIAS_SHOULD_EXPAND_TO_CLASS!>@Suppress("TYPEALIAS_SHOULD_EXPAND_TO_CLASS") T<!>

interface A

interface B : <!SUPERTYPE_INITIALIZED_IN_INTERFACE!>@Suppress("SUPERTYPE_INITIALIZED_IN_INTERFACE") A<!>()

data class D @Suppress("DATA_CLASS_VARARG_PARAMETER") constructor(<!DATA_CLASS_VARARG_PARAMETER!>vararg val x: String<!>)
