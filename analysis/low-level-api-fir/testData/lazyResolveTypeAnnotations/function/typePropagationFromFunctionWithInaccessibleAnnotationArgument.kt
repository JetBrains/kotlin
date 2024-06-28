// SKIP_WHEN_OUT_OF_CONTENT_ROOT
// MODULE: m1
// FILE: declaration.kt
@Target(AnnotationTarget.TYPE)
annotation class Anno(val number: Int)
private const val privateConstant = 0
internal const val internalConstant = 1

fun withType(): @Anno(internalConstant) List<@Anno(privateConstant) Int> {

}

// MODULE: m2(m1)
// FILE: usafe.kt
fun impli<caret>citType() = withType()