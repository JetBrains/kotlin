// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocksAndExtensions
// FILE: c.kt
package c
abstract class C

private companion fun C.private() {}
<!WRONG_MODIFIER_TARGET!>protected<!> companion fun C.protected() {}
public companion fun C.public() {}
internal companion fun C.internal() {}

<!WRONG_MODIFIER_TARGET!>abstract<!> companion fun C.abstract()
<!WRONG_MODIFIER_TARGET!>open<!> companion fun C.open() {}
<!WRONG_MODIFIER_TARGET!>final<!> companion fun C.final() {}
<!WRONG_MODIFIER_TARGET!>override<!> companion fun C.override() {}

suspend companion fun C.suspend() {}
external companion fun C.external(): String
<!WRONG_MODIFIER_TARGET!>lateinit<!> companion fun C.lateinit() {}
tailrec companion fun C.tailrec() { C.tailrec() }
<!WRONG_MODIFIER_TARGET!>const<!> companion fun C.const() {}
<!NOT_A_MULTIPLATFORM_COMPILATION!>expect<!> companion fun C.expect(): String
<!NOT_A_MULTIPLATFORM_COMPILATION!>actual<!> companion fun C.actual() {}
<!NOTHING_TO_INLINE!>inline<!> companion fun C.inline() {}

// FILE: d.kt
package d

abstract class D

private companion val D.private = 1
<!WRONG_MODIFIER_TARGET!>protected<!> companion val D.protected = 1
public companion val D.public = 1
internal companion val D.internal = 1

<!WRONG_MODIFIER_TARGET!>abstract<!> companion val D.abstract = 1
<!WRONG_MODIFIER_TARGET!>open<!> companion val D.bar = 1
<!WRONG_MODIFIER_TARGET!>final<!> companion val D.baz = 1
<!WRONG_MODIFIER_TARGET!>override<!> companion val D.qux = 1

<!WRONG_MODIFIER_TARGET!>suspend<!> companion val D.suspend = 1
<!WRONG_MODIFIER_TARGET!>external<!> companion val D.external: String
lateinit companion var D.lateinit: Any
<!WRONG_MODIFIER_TARGET!>tailrec<!> companion val D.tailrec = 1
const companion val D.const = 1
<!NOT_A_MULTIPLATFORM_COMPILATION!>expect<!> companion val D.expect: String
<!NOT_A_MULTIPLATFORM_COMPILATION!>actual<!> companion val D.actual = 1
inline companion val D.inline get() = 1

/* GENERATED_FIR_TAGS: actual, classDeclaration, companion functionDeclaration,C. override, suspend */
