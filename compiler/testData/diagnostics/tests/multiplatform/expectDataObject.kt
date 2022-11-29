// LANGUAGE: +DataObjects
// MODULE: m1-common
// FILE: common.kt
<!INCOMPATIBLE_MODIFIERS, INCOMPATIBLE_MODIFIERS{JVM}!>expect<!> <!INCOMPATIBLE_MODIFIERS, INCOMPATIBLE_MODIFIERS{JVM}!>data<!> object DataObject

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
actual data object DataObject
