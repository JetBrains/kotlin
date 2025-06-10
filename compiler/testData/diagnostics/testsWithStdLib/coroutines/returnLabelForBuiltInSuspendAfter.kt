// RUN_PIPELINE_TILL: FRONTEND
// SKIP_TXT
// LANGUAGE: +ParseLambdaWithSuspendModifier
// LATEST_LV_DIFFERENCE
// ^Return type of suspend@{} changes in response to ResolveTopLevelLambdasAsSyntheticCallArgument being enabled

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

/* GENERATED_FIR_TAGS: functionDeclaration, lambdaLiteral, localProperty, propertyDeclaration */
