// !DIAGNOSTICS: -TYPE_PARAMETER_AS_REIFIED -TYPE_PARAMETER_OF_PROPERTY_NOT_USED_IN_RECEIVER -UNUSED_VARIABLE -UNUSED_PARAMETER

fun <T> test1() = <!OTHER_ERROR!>T<!>::class
fun <T : Any> test2() = <!OTHER_ERROR!>T<!>::class

val <T> test3 = <!OTHER_ERROR!>T<!>::class
val <T> test4 get() = <!OTHER_ERROR!>T<!>::class

fun <T> test5() = listOf(<!OTHER_ERROR!>T<!>::class)

fun <T> test6(): kotlin.reflect.KClass<T> = <!OTHER_ERROR!>T<!>::class
fun <T> test7(): kotlin.reflect.KClass<*> = <!OTHER_ERROR!>T<!>::class
fun test8() = <!NULLABLE_TYPE_IN_CLASS_LITERAL_LHS!>String?::class<!>

fun <T> listOf(e: T): List<T> = null!!

fun <L> locals() {
    fun <T> test1() = <!OTHER_ERROR!>T<!>::class
    fun <T : Any> test2() = <!OTHER_ERROR!>T<!>::class

    val test3 = <!OTHER_ERROR!>L<!>::class
    fun test4() = <!OTHER_ERROR!>L<!>::class
}
