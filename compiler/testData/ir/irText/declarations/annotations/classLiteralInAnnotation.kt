import kotlin.reflect.KClass

annotation class A(val klass: KClass<*>)

class C

@A(C::class) fun test1() {}

inline fun <reified T> test2() =
    @A(T::class) object {}

inline var <reified T> T.test3
    get() = @A(T::class) object {}
    set(v) { @A(T::class) object {} }