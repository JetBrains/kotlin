// RUN_PIPELINE_TILL: FRONTEND
// MODULE: m1-common
// FILE: common.kt

import kotlin.reflect.KClass

expect annotation class Anno(
    // CLASS_LITERAL_LHS_NOT_A_CLASS is reported because we have multiple platforms one of which isn't JVM.
    val ka: KClass<*> = Array<Array<Array<Int>>>::class,
)

enum class E { E1, E2, E3 }

annotation class A(val value: String)

@Anno
fun test() {}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual typealias Anno = Jnno

// FILE: Jnno.java

public @interface Jnno {
    Class<?> ka() default Integer[][][].class;
}
