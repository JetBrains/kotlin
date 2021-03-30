// FIR_IGNORE
interface Any

//                                            T?
//                                            │
inline fun <reified T : Any> Any.safeAs(): T? = this as? T

abstract class Summator {
    abstract fun <T> plus(first: T, second: T): T
}
