// SKIP_TXT
fun bar() {
    suspend {
        println()
    }

    @Ann suspend {
        println()
    }

    suspend @Ann {
        println()
    }

    kotlin.<!NON_MODIFIER_FORM_FOR_BUILT_IN_SUSPEND!>suspend<!> {

    }

    <!NON_MODIFIER_FORM_FOR_BUILT_IN_SUSPEND!>suspend<!>() {
        println()
    }

    <!NON_MODIFIER_FORM_FOR_BUILT_IN_SUSPEND!>suspend<!>({ println() })

    <!NON_MODIFIER_FORM_FOR_BUILT_IN_SUSPEND!>suspend<!><Unit> {
        println()
    }

    val <!UNUSED_VARIABLE!>w<!>: (suspend () -> Int) -> Any? = ::<!NON_MODIFIER_FORM_FOR_BUILT_IN_SUSPEND!>suspend<!>
}

@Target(AnnotationTarget.EXPRESSION)
annotation class Ann
