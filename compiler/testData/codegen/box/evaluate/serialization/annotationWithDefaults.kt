// MODULE: lib
// FILE: lib.kt

import kotlin.reflect.KClass

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class IntegerNumberValid(
    val message: String = "Has illegal integer number value",
    val groups: Array<KClass<*>> = [],

    val minimum: Long = Long.MIN_VALUE,
    val maximum: Long = Long.MAX_VALUE,

    val minMaxArray: LongArray = longArrayOf(Long.MIN_VALUE, Long.MAX_VALUE),
    val minMaxArrayCollection: LongArray = [Long.MIN_VALUE, Long.MAX_VALUE],

    val laterConstant: String = VALUE
)

const val VALUE = "hello"

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class AnnotationWithDefault(val str: String = "Str" + "ing")

@AnnotationWithDefault()
class A

@AnnotationWithDefault("Other")
class B

// MODULE: main(lib)
// FILE: main.kt

fun box(): String {
    return "OK"
}
