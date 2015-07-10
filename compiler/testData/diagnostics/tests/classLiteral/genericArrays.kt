import kotlin.reflect.KClass

fun f1<T>(): KClass<Array<T>> = <!CLASS_LITERAL_LHS_NOT_A_CLASS!>Array<T>::class<!>
fun f2<T>(): KClass<Array<Array<T>>> = <!CLASS_LITERAL_LHS_NOT_A_CLASS!>Array<Array<T>>::class<!>
inline fun f3<reified T>() = Array<T>::class
inline fun f4<reified T>() = Array<Array<T>>::class
fun f5(): KClass<Array<Any>> = <!CLASS_LITERAL_LHS_NOT_A_CLASS!>Array<*>::class<!>
fun f6(): KClass<Array<Int?>> = Array<Int?>::class