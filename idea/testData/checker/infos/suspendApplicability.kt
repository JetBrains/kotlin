// !DIAGNOSTICS: -UNUSED_PARAMETER -NOTHING_TO_INLINE
<error descr="[WRONG_MODIFIER_TARGET] Modifier 'suspend' is not applicable to 'top level function'"><info descr="null">suspend</info></error> fun notMember(<warning descr="[UNUSED_PARAMETER] Parameter 'x' is never used">x</warning>: Continuation<Int>) {

}

class Controller {
    <info descr="null">suspend</info> fun valid(<warning descr="[UNUSED_PARAMETER] Parameter 'x' is never used">x</warning>: Continuation<Int>) {

    }

    <warning descr="[NOTHING_TO_INLINE] Expected performance impact of inlining 'public final inline suspend fun inlineFun(x: Continuation<Int>): Unit defined in Controller' can be insignificant. Inlining works best for functions with lambda parameters"><info descr="null">inline</info></warning> <error descr="[INAPPLICABLE_MODIFIER] 'suspend' modifier is inapplicable. The reason is that inline suspend functions are not supported"><info descr="null">suspend</info></error> fun inlineFun(<warning descr="[UNUSED_PARAMETER] Parameter 'x' is never used">x</warning>: Continuation<Int>) {

    }

    <error descr="[INAPPLICABLE_MODIFIER] 'suspend' modifier is inapplicable. The reason is that last parameter of suspend function should have a type of Continuation<T>"><info descr="null">suspend</info></error> fun noParams() {

    }

    <error descr="[INAPPLICABLE_MODIFIER] 'suspend' modifier is inapplicable. The reason is that last parameter of suspend function should have a type of Continuation<T>"><info descr="null">suspend</info></error> fun wrongParam(<warning descr="[UNUSED_PARAMETER] Parameter 'x' is never used">x</warning>: Collection<Int>) {

    }

    <info descr="null">suspend</info> fun starProjection(<warning descr="[UNUSED_PARAMETER] Parameter 'x' is never used">x</warning>: Continuation<*>) {

    }
}
