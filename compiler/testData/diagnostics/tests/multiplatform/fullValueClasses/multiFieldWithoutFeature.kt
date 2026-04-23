// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// LANGUAGE: -FullValueClasses, +MultiPlatformProjects
// ALLOW_KOTLIN_PACKAGE

// MODULE: m1-common
// FILE: common.kt

package kotlin.jvm

annotation class JvmInline


expect <!UNSUPPORTED_FEATURE!>value<!> class Final(val value1: String, val value2: String)
<!UNSUPPORTED_FEATURE, UNSUPPORTED_FEATURE{METADATA}!>value<!> class Final1(val value1: String, val value2: String)
expect value class Final2(val value1: String)
<!UNSUPPORTED_FEATURE!>value<!> class Final3(val value1: String)
expect value class Final4<!INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE!>()<!>
<!VALUE_CLASS_WITHOUT_JVM_INLINE_ANNOTATION!>value<!> class Final5<!INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE, INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE{METADATA}!>()<!>
expect <!ABSENCE_OF_PRIMARY_CONSTRUCTOR_FOR_VALUE_CLASS!>value<!> class Final6
<!ABSENCE_OF_PRIMARY_CONSTRUCTOR_FOR_VALUE_CLASS, ABSENCE_OF_PRIMARY_CONSTRUCTOR_FOR_VALUE_CLASS{METADATA}, VALUE_CLASS_WITHOUT_JVM_INLINE_ANNOTATION!>value<!> class Final7

@JvmInline
expect value class Final8<!INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE!>(val value1: String, val value2: String)<!>
@JvmInline
value class Final9<!INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE, INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE{METADATA}!>(val value1: String, val value2: String)<!>
@JvmInline
expect value class Final10(val value1: String)
@JvmInline
value class Final11(val value1: String)
@JvmInline
expect value class Final12<!INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE!>()<!>
@JvmInline
value class Final13<!INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE, INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE{METADATA}!>()<!>
@JvmInline
expect <!ABSENCE_OF_PRIMARY_CONSTRUCTOR_FOR_VALUE_CLASS!>value<!> class Final14
@JvmInline
<!ABSENCE_OF_PRIMARY_CONSTRUCTOR_FOR_VALUE_CLASS, ABSENCE_OF_PRIMARY_CONSTRUCTOR_FOR_VALUE_CLASS{METADATA}!>value<!> class Final15

expect <!INLINE_CLASS_DEPRECATED!>inline<!> class Final16<!INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE!>(val value1: String, val value2: String)<!>
<!INLINE_CLASS_DEPRECATED!>inline<!> class Final17<!INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE, INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE{METADATA}!>(val value1: String, val value2: String)<!>
expect <!INLINE_CLASS_DEPRECATED!>inline<!> class Final18(val value1: String)
<!INLINE_CLASS_DEPRECATED!>inline<!> class Final19(val value1: String)
expect <!INLINE_CLASS_DEPRECATED!>inline<!> class Final20<!INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE!>()<!>
<!INLINE_CLASS_DEPRECATED!>inline<!> class Final21<!INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE, INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE{METADATA}!>()<!>
expect <!ABSENCE_OF_PRIMARY_CONSTRUCTOR_FOR_VALUE_CLASS, INLINE_CLASS_DEPRECATED!>inline<!> class Final22
<!ABSENCE_OF_PRIMARY_CONSTRUCTOR_FOR_VALUE_CLASS, ABSENCE_OF_PRIMARY_CONSTRUCTOR_FOR_VALUE_CLASS{METADATA}, INLINE_CLASS_DEPRECATED!>inline<!> class Final23


// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
package kotlin.jvm

actual <!UNSUPPORTED_FEATURE!>value<!> class Final actual constructor(val value1: String, val value2: String)
actual <!UNSUPPORTED_FEATURE!>value<!> class Final2 actual constructor(val value1: String)
actual <!VALUE_CLASS_WITHOUT_JVM_INLINE_ANNOTATION!>value<!> class Final4 <!INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE!>actual constructor()<!>
actual <!ABSENCE_OF_PRIMARY_CONSTRUCTOR_FOR_VALUE_CLASS, VALUE_CLASS_WITHOUT_JVM_INLINE_ANNOTATION!>value<!> class Final6

@JvmInline
actual value class Final8 <!INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE!>actual constructor(val value1: String, val value2: String)<!>
@JvmInline
actual value class Final10 actual constructor(val value1: String)
@JvmInline
actual value class Final12 <!INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE!>actual constructor()<!>
@JvmInline
actual <!ABSENCE_OF_PRIMARY_CONSTRUCTOR_FOR_VALUE_CLASS!>value<!> class Final14

actual <!INLINE_CLASS_DEPRECATED!>inline<!> class Final16 <!INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE!>actual constructor(val value1: String, val value2: String)<!>
actual <!INLINE_CLASS_DEPRECATED!>inline<!> class Final18 actual constructor(val value1: String)
actual <!INLINE_CLASS_DEPRECATED!>inline<!> class Final20 <!INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE!>actual constructor()<!>
actual <!ABSENCE_OF_PRIMARY_CONSTRUCTOR_FOR_VALUE_CLASS, INLINE_CLASS_DEPRECATED!>inline<!> class Final22

/* GENERATED_FIR_TAGS: actual, annotationDeclaration, classDeclaration, expect, functionDeclaration, integerLiteral,
primaryConstructor, propertyDeclaration, stringLiteral, value */
