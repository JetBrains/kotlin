import kotlin.reflect.KClass

fun <T> f1(): KClass<Array<T>> = <!CLASS_LITERAL_LHS_NOT_A_CLASS!>Array<T>::class<!>
fun <T> f2(): KClass<Array<Array<T>>> = <!CLASS_LITERAL_LHS_NOT_A_CLASS!>Array<Array<T>>::class<!>
inline fun <reified T> f3() = Array<T>::class
inline fun <reified T> f4() = Array<Array<T>>::class
fun f5(): KClass<Array<Any>> = <!CLASS_LITERAL_LHS_NOT_A_CLASS, TYPE_MISMATCH!>Array<*>::class<!>
fun f6(): KClass<Array<Int?>> = Array<Int?>::class
fun f7() = <!CLASS_LITERAL_LHS_NOT_A_CLASS!>Array<List<String>>::class<!>
fun f8() = <!CLASS_LITERAL_LHS_NOT_A_CLASS!>Array<List<String>?>::class<!>
fun f9() = <!CLASS_LITERAL_LHS_NOT_A_CLASS!>Array<List<*>?>::class<!>
