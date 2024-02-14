import kotlin.reflect.KClass

typealias MyString = String

fun test(k: KClass<out MyString>) {
    k::class.java
}

@Suppress(<!ERROR_SUPPRESSION!>"UPPER_BOUND_VIOLATED"<!>)
public val <T> KClass<T>.java: Class<T> get() = TODO()
