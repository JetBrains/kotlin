import kotlin.reflect.KClass

fun <T> f1(): KClass<Array<T>> = Array<T>::class
fun <T> f2(): KClass<Array<Array<T>>> = Array<Array<T>>::class
inline fun <reified T> f3() = Array<T>::class
inline fun <reified T> f4() = Array<Array<T>>::class
fun f5(): KClass<Array<Any>> = Array<*>::class
fun f6(): KClass<Array<Int?>> = Array<Int?>::class
fun f7() = Array<List<String>>::class
fun f8() = Array<List<String>?>::class
fun f9() = Array<List<*>?>::class
