// FIR_IDENTICAL
// SKIP_TXT

infix fun Int.suspend(c: () -> Unit) { c() }

fun bar() {
    1 <!MODIFIER_FORM_FOR_NON_BUILT_IN_SUSPEND_FUN_ERROR!>suspend<!> fun() {
        println()
    }

    1 <!SYNTAX!>@Ann suspend fun()<!> {
        println()
    }

    1 <!MODIFIER_FORM_FOR_NON_BUILT_IN_SUSPEND_FUN_ERROR!>suspend<!> @Ann fun() {
        println()
    }
}

@Target(AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
annotation class Ann

fun main(suspend: WLambdaInvoke) {
    1 <!MODIFIER_FORM_FOR_NON_BUILT_IN_SUSPEND_FUN_ERROR!>suspend<!> fun() {}
}

class WLambdaInvoke {
    operator fun Int.invoke(l: () -> Unit) {}
}
