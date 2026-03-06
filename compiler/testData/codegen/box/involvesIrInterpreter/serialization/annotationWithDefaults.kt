// WASM_FAILS_IN_SINGLE_MODULE_MODE

// MODULE: lib
// FILE: lib.kt

import kotlin.reflect.KClass

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class IntegerNumberValid(
    val message: String = <!EVALUATED{FIR}("Has illegal integer number value")!>"Has illegal integer number value"<!>,
    val groups: Array<KClass<*>> = [],

    val minimum: Long = Long.<!EVALUATED("-9223372036854775808")!>MIN_VALUE<!>,
    val maximum: Long = Long.<!EVALUATED("9223372036854775807")!>MAX_VALUE<!>,

    val minMaxArray: LongArray = <!EVALUATED{IR}("[-9223372036854775808.toLong(), 9223372036854775807.toLong()]")!>longArrayOf(Long.<!EVALUATED{IR}("-9223372036854775808")!>MIN_VALUE<!>, Long.<!EVALUATED{IR}("9223372036854775807")!>MAX_VALUE<!>)<!>,
    val minMaxArrayCollection: LongArray = <!EVALUATED{IR}("[-9223372036854775808.toLong(), 9223372036854775807.toLong()]")!>[Long.<!EVALUATED{IR}("-9223372036854775808")!>MIN_VALUE<!>, Long.<!EVALUATED{IR}("9223372036854775807")!>MAX_VALUE<!>]<!>,

    val laterConstant: String = <!EVALUATED("hello")!>VALUE<!>
)

const val VALUE = <!EVALUATED("hello")!>"hello"<!>

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class AnnotationWithDefault(val str: String = <!EVALUATED("String")!>"Str" + "ing"<!>)

@AnnotationWithDefault()
class A

@AnnotationWithDefault(<!EVALUATED("Other")!>"Other"<!>)
class B

// MODULE: main(lib)
// FILE: main.kt

fun box(): String {
    return "OK"
}
