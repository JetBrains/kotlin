// LANGUAGE: +DataObjects
// MODULE: m1-common
// FILE: common.kt
<!INCOMPATIBLE_MODIFIERS!>expect<!> <!INCOMPATIBLE_MODIFIERS!>data<!> object DataObject

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
actual data object DataObject
