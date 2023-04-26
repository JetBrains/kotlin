// TARGET_BACKEND: JVM_IR
// TARGET_BACKEND: JS_IR
// TARGET_BACKEND: NATIVE

// MODULE: lib
// FILE: lib.kt

import kotlin.reflect.KClass

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class IntegerNumberValid(
    val message: String = <!EVALUATED("Has illegal integer number value")!>"Has illegal integer number value"<!>,
    val groups: Array<KClass<*>> = [],

    val minimum: Long = Long.<!EVALUATED("-9223372036854775808")!>MIN_VALUE<!>,
    val maximum: Long = Long.<!EVALUATED("9223372036854775807")!>MAX_VALUE<!>,

    val minMaxArray: LongArray = longArrayOf(Long.<!EVALUATED("-9223372036854775808")!>MIN_VALUE<!>, Long.<!EVALUATED("9223372036854775807")!>MAX_VALUE<!>),
    val minMaxArrayCollection: LongArray = [Long.<!EVALUATED("-9223372036854775808")!>MIN_VALUE<!>, Long.<!EVALUATED("9223372036854775807")!>MAX_VALUE<!>],
)

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class AnnotationWithDefault(val str: String = "Str" <!EVALUATED("String")!>+ "ing"<!>)

@AnnotationWithDefault()
class A

@AnnotationWithDefault(<!EVALUATED("Other")!>"Other"<!>)
class B

// MODULE: main
// FILE: main.kt

fun box(): String {
    return "OK"
}
