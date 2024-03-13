// SKIP_TXT
fun bar() {
    <!NON_MODIFIER_FORM_FOR_BUILT_IN_SUSPEND!>suspend<!> {
        <!RETURN_FOR_BUILT_IN_SUSPEND!>return@suspend<!>
    }

    <!NON_MODIFIER_FORM_FOR_BUILT_IN_SUSPEND!>suspend<!> {
        run {
            <!RETURN_FOR_BUILT_IN_SUSPEND!>return@suspend<!>
        }
    }

    <!NON_MODIFIER_FORM_FOR_BUILT_IN_SUSPEND!>suspend<!> l@{
        return@l
    }

    <!NON_MODIFIER_FORM_FOR_BUILT_IN_SUSPEND!>suspend<!> suspend@{
        return@suspend
    }

    val x = suspend@{
        <!NON_MODIFIER_FORM_FOR_BUILT_IN_SUSPEND!>suspend<!> {
            // Might be resolved to outer lambda, but doesn't make sense because suspend-lambdas here is noinline
            <!RETURN_FOR_BUILT_IN_SUSPEND!>return@suspend<!>
        }
    }
}
