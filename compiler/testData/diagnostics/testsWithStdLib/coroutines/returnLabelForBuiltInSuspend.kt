// SKIP_TXT
fun bar() {
    suspend {
        <!RETURN_FOR_BUILT_IN_SUSPEND!>return@suspend<!>
    }

    suspend {
        run {
            <!RETURN_FOR_BUILT_IN_SUSPEND!>return@suspend<!>
        }
    }

    suspend l@{
        return@l
    }

    suspend suspend@{
        return@suspend
    }

    val x = suspend@{
        suspend {
            // Might be resolved to outer lambda, but doesn't make sense because suspend-lambdas here is noinline
            <!RETURN_FOR_BUILT_IN_SUSPEND!>return<!LABEL_NAME_CLASH!>@suspend<!><!>
        }
    }
}
