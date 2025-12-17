// RUN_PIPELINE_TILL: BACKEND

// KT-31464

// MODULE: m1-common
// FILE: common.kt
<!INLINE_PROPERTY_WITH_BACKING_FIELD, INLINE_PROPERTY_WITH_BACKING_FIELD{JVM}!>expect inline val <T> T.x: Int<!>

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
actual inline val <T> T.x get() = 10

/* GENERATED_FIR_TAGS: actual, expect, getter, integerLiteral, nullableType, propertyDeclaration,
propertyWithExtensionReceiver, typeParameter */
