import kotlin.reflect.KClass

class SomeClass

inline fun <reified K> foo(klass: KClass<*>): K = null!!

val some: Map<String, String> by lazy {
    SomeClass::class.let {
        foo(it)
    }
}
