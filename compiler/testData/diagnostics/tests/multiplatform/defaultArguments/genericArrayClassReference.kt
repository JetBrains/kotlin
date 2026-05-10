// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt

import kotlin.reflect.KClass

expect annotation class Anno(
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

/* GENERATED_FIR_TAGS: actual, annotationDeclaration, classReference, enumDeclaration, enumEntry, expect,
functionDeclaration, javaType, primaryConstructor, propertyDeclaration, starProjection, typeAliasDeclaration */
