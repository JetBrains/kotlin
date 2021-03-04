// !DIAGNOSTICS: -TYPE_PARAMETER_AS_REIFIED -TYPE_PARAMETER_OF_PROPERTY_NOT_USED_IN_RECEIVER -UNUSED_VARIABLE -UNUSED_PARAMETER

fun <T> test1() = <!TYPE_PARAMETER_IS_NOT_AN_EXPRESSION!>T<!>::class
fun <T : Any> test2() = <!TYPE_PARAMETER_IS_NOT_AN_EXPRESSION!>T<!>::class

val <T> test3 = <!TYPE_PARAMETER_IS_NOT_AN_EXPRESSION!>T<!>::class
val <T> test4 get() = <!TYPE_PARAMETER_IS_NOT_AN_EXPRESSION!>T<!>::class

fun <T> test5() = listOf(T::class)

fun <T> test6(): kotlin.reflect.KClass<T> = <!TYPE_PARAMETER_IS_NOT_AN_EXPRESSION!>T<!>::class
fun <T> test7(): kotlin.reflect.KClass<*> = <!TYPE_PARAMETER_IS_NOT_AN_EXPRESSION!>T<!>::class
fun test8() = <!NULLABLE_TYPE_IN_CLASS_LITERAL_LHS!>String?::class<!>

fun <T> listOf(e: T): List<T> = null!!

fun <L> locals() {
    fun <T> test1() = <!TYPE_PARAMETER_IS_NOT_AN_EXPRESSION!>T<!>::class
    fun <T : Any> test2() = <!TYPE_PARAMETER_IS_NOT_AN_EXPRESSION!>T<!>::class

    val test3 = <!TYPE_PARAMETER_IS_NOT_AN_EXPRESSION!>L<!>::class
    fun test4() = <!TYPE_PARAMETER_IS_NOT_AN_EXPRESSION!>L<!>::class
}
