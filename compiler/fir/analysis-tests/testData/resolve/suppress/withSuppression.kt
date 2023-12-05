const val x = "123"
    @Suppress(<!ERROR_SUPPRESSION!>"CONST_VAL_WITH_GETTER"<!>)
    get() = field

val y = "789"

const val z = @Suppress(<!ERROR_SUPPRESSION!>"NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION"<!>) y

@Target(AnnotationTarget.TYPE)
annotation class Ann

fun foo(): @Suppress(<!ERROR_SUPPRESSION!>"REPEATED_ANNOTATION"<!>) @Ann @Ann Int = 42

typealias Alias<T> = @Suppress(<!ERROR_SUPPRESSION!>"TYPEALIAS_SHOULD_EXPAND_TO_CLASS"<!>) T

interface A

interface B : @Suppress(<!ERROR_SUPPRESSION!>"SUPERTYPE_INITIALIZED_IN_INTERFACE"<!>) A<!NO_CONSTRUCTOR!>()<!>

data class D @Suppress(<!ERROR_SUPPRESSION!>"DATA_CLASS_VARARG_PARAMETER"<!>) constructor(vararg val x: String)
