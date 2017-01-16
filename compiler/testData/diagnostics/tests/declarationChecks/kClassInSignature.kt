// !DIAGNOSTICS: -TYPE_PARAMETER_AS_REIFIED -TYPE_PARAMETER_OF_PROPERTY_NOT_USED_IN_RECEIVER -UNUSED_VARIABLE -UNUSED_PARAMETER

fun <T> <!KCLASS_WITH_NULLABLE_TYPE_PARAMETER_IN_SIGNATURE!>test1<!>() = T::class
fun <T : Any> test2() = T::class

val <T> <!KCLASS_WITH_NULLABLE_TYPE_PARAMETER_IN_SIGNATURE!>test3<!> = T::class
val <T> <!KCLASS_WITH_NULLABLE_TYPE_PARAMETER_IN_SIGNATURE!>test4<!> get() = T::class

fun <T> <!KCLASS_WITH_NULLABLE_TYPE_PARAMETER_IN_SIGNATURE!>test5<!>() = listOf(T::class)

fun <T> test6(): kotlin.reflect.KClass<<!UPPER_BOUND_VIOLATED!>T<!>> = T::class
fun <T> test7(): kotlin.reflect.KClass<*> = T::class
fun <!KCLASS_WITH_NULLABLE_ARGUMENT_IN_SIGNATURE!>test8<!>() = <!NULLABLE_TYPE_IN_CLASS_LITERAL_LHS!>String?::class<!>

fun <T> listOf(e: T): List<T> = null!!

fun <L> locals() {
    fun <T> test1() = T::class
    fun <T : Any> test2() = T::class

    val test3 = L::class
    fun test4() = L::class
}
