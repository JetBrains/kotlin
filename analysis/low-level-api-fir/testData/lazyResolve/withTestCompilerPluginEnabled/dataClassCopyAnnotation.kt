// MEMBER_NAME_FILTER: copy
// WITH_FIR_TEST_COMPILER_PLUGIN
// SKIP_WHEN_OUT_OF_CONTENT_ROOT
// FILE: main.kt
package pack

annotation class MyAnno(val value: Int)

data class MyCl<caret>ass(
    @MyAnno(1)
    val disabled: Boolean,
)
