// SKIP_WHEN_OUT_OF_CONTENT_ROOT
// MODULE: m1
// FILE: declaration.kt
private const val privateConstant = "0"
internal const val internalConstant = "1"
const val regularConstant = "2"

@Target(AnnotationTarget.TYPE)
annotation class Anno(val message: String)

val nullablePropertyWithAnnotatedType: @Anno(privateConstant) List<@Anno(internalConstant) List<@Anno(regularConstant) Int>>?
    get() = null

// MODULE: m2(m1)
// FILE: usafe.kt

val proper<caret>tyToResolve: String
    get() = nullablePropertyWithAnnotatedType?.let { " ($it)" } ?: ""