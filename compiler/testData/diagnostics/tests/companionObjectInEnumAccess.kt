// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-15672
// WITH_STDLIB
// RENDER_DIAGNOSTICS_FULL_TEXT

enum class MyEnum {
    VALUE;

    init {
        println(<!UNINITIALIZED_VARIABLE!>staticValue<!>)
    }

    companion object {
        private val staticValue = VALUE
    }
}
