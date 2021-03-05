// !DIAGNOSTICS: -TYPE_PARAMETER_AS_REIFIED -TYPE_PARAMETER_OF_PROPERTY_NOT_USED_IN_RECEIVER -UNUSED_VARIABLE -UNUSED_PARAMETER

fun <T> test1() = T::class
fun <T : Any> test2() = T::class

val <T> test3 = T::class
val <T> test4 get() = T::class

fun <T> test5() = listOf(T::class)

fun <T> test6(): kotlin.reflect.KClass<T> = T::class
fun <T> test7(): kotlin.reflect.KClass<*> = T::class
fun test8() = <!NULLABLE_TYPE_IN_CLASS_LITERAL_LHS!>String?::class<!>

fun <T> listOf(e: T): List<T> = null!!

fun <L> locals() {
    fun <T> test1() = T::class
    fun <T : Any> test2() = T::class

    val test3 = L::class
    fun test4() = L::class
}
