// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt

import kotlin.reflect.KClass

expect annotation class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Anno<!>(
    // CLASS_LITERAL_LHS_NOT_A_CLASS is reported because we have multiple platforms one of which isn't JVM.
    val ka: KClass<*> = <!CLASS_LITERAL_LHS_NOT_A_CLASS!>Array<Array<Array<Int>>>::class<!>,
)

enum class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>E<!> { E1, E2, E3 }

annotation class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>A<!>(val value: String)

<!CONFLICTING_OVERLOADS!>@Anno
fun test()<!> {}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual typealias Anno = Jnno

// FILE: Jnno.java

public @interface Jnno {
    Class<?> ka() default Integer[][][].class;
}
