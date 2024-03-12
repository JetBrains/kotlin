import kotlin.reflect.KClass

typealias MyString = String

fun test(k: KClass<out MyString>) {
    k::class.<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>java<!>
}

@Suppress(<!ERROR_SUPPRESSION!>"UPPER_BOUND_VIOLATED"<!>)
public val <T> KClass<T>.java: Class<T> get() = TODO()
