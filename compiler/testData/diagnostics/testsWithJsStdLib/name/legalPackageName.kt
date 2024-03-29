// FIR_IDENTICAL
// KT-67056: for slash.kt, the following klib serializer crash happened:
//   java.nio.file.NoSuchFileException: /private/var/folders/rz/qbhyvlgx1mbgf73mq__ndj1r0000gn/T/js.testsProject_test_11328237796686026218/org.jetbrains.kotlin.js.test.fir.FirPsiJsOldFrontendDiagnosticsWithBackendTestGenerated$TestsWithJsStdLib$NametestLegalPackageName/outputKlibDir/legalPackageName/default/linkdata/package_/0_/.knm
// SKIP_KLIB_SERIALIZATION
// !LANGUAGE: +JsAllowInvalidCharsIdentifiersEscaping

// FILE: slashes.kt
package a.`//`.b.`/`.c
class Slashes

// FILE: slash.kt
package `/`
class Slash

// FILE: space.kt
package ` `
class Space

// FILE: less.kt
package `<`
class Less

// FILE: more.kt
package `>`
class More

// FILE: dash.kt
package `-`
class Dash

// FILE: question.kt
package `?`
class Question
