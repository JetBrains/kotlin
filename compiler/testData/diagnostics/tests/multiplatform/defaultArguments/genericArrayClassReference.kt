// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FRONTEND
// MODULE: m1-common
// FILE: common.kt

import kotlin.reflect.KClass

expect annotation class Anno(
    // CLASS_LITERAL_LHS_NOT_A_CLASS is reported because we have multiple platforms one of which isn't JVM.
    val ka: KClass<*> = <!CLASS_LITERAL_LHS_NOT_A_CLASS, CLASS_LITERAL_LHS_NOT_A_CLASS{JS}!>Array<Array<Array<Int>>>::class<!>,
)

enum class E { E1, E2, E3 }

annotation class A(val value: String)

@Anno
fun test() {}

// MODULE: m2-js()()(m1-common)
// FILE: js.kt
import kotlin.reflect.KClass

actual annotation class Anno(
    actual val ka: KClass<*> = <!CLASS_LITERAL_LHS_NOT_A_CLASS!>Array<Array<Array<Int>>>::class<!>,
)

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual typealias Anno = Jnno

// FILE: Jnno.java

public @interface Jnno {
    Class<?> ka() default Integer[][][].class;
}
