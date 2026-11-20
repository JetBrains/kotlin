// METADATA_TARGET_PLATFORMS: JVM, JS
// LANGUAGE: +AllowExpectValueClassesWithNoPrimaryConstructor +FullValueClasses
// ISSUE: KT-90593

// MODULE: lib-common
import kotlin.jvm.JvmInline

expect inline value class Val
expect inline value class ForRef(val x: Int)
expect value class Full

@JvmInline
expect value class JvmInl

// MODULE: app-common()()(lib-common)
fun fooVal(<!FORBIDDEN_VARARG_PARAMETER_TYPE!>vararg<!> vl: Val) = Unit
fun fooForRef(<!FORBIDDEN_VARARG_PARAMETER_TYPE!>vararg<!> vl: ForRef) = Unit
fun fooFull(vararg vl: Full) = Unit
fun fooJvmInl(<!FORBIDDEN_VARARG_PARAMETER_TYPE!>vararg<!> vl: JvmInl) = Unit
