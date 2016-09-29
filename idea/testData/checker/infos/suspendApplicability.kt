// !DIAGNOSTICS: -UNUSED_PARAMETER -NOTHING_TO_INLINE
<error descr="[INAPPLICABLE_MODIFIER] 'suspend' modifier is inapplicable. The reason is that function must be either a class member or an extension"><info descr="null">suspend</info></error> fun notMember(<warning descr="[UNUSED_PARAMETER] Parameter 'x' is never used">x</warning>: Continuation<Int>) {

}

<error descr="[INAPPLICABLE_MODIFIER] 'suspend' modifier is inapplicable. The reason is that controller class must be annotated with AllowSuspendExtensions annotation"><info descr="null">suspend</info></error> fun String.wrongExtension(<warning descr="[UNUSED_PARAMETER] Parameter 'x' is never used">x</warning>: Continuation<Int>) {
}

<info descr="null">suspend</info> fun Controller.correctExtension(<warning descr="[UNUSED_PARAMETER] Parameter 'x' is never used">x</warning>: Continuation<Int>) {
}

@AllowSuspendExtensions
class Controller {
    <info descr="null">suspend</info> fun valid(<warning descr="[UNUSED_PARAMETER] Parameter 'x' is never used">x</warning>: Continuation<Int>) {

    }

    <warning descr="[NOTHING_TO_INLINE] Expected performance impact of inlining 'public final inline suspend fun inlineFun(x: Continuation<Int>): Unit defined in Controller' can be insignificant. Inlining works best for functions with lambda parameters"><info descr="null">inline</info></warning> <info descr="null">suspend</info> fun inlineFun(<warning descr="[UNUSED_PARAMETER] Parameter 'x' is never used">x</warning>: Continuation<Int>) {

    }

    <error descr="[INAPPLICABLE_MODIFIER] 'suspend' modifier is inapplicable. The reason is that last parameter of suspend function should have a type of Continuation<T>"><info descr="null">suspend</info></error> fun noParams() {

    }

    <error descr="[INAPPLICABLE_MODIFIER] 'suspend' modifier is inapplicable. The reason is that last parameter of suspend function should have a type of Continuation<T>"><info descr="null">suspend</info></error> fun wrongParam(<warning descr="[UNUSED_PARAMETER] Parameter 'x' is never used">x</warning>: Collection<Int>) {

    }

    <error descr="[INAPPLICABLE_MODIFIER] 'suspend' modifier is inapplicable. The reason is that Continuation<*> is prohibited as a last parameter of suspend function"><info descr="null">suspend</info></error> fun starProjection(<warning descr="[UNUSED_PARAMETER] Parameter 'x' is never used">x</warning>: Continuation<*>) {

    }

    <error descr="[INAPPLICABLE_MODIFIER] 'suspend' modifier is inapplicable. The reason is that last parameter of suspend function should have a type of Continuation<T>"><info descr="null">suspend</info></error> fun varargs(<info descr="null">vararg</info> <warning descr="[UNUSED_PARAMETER] Parameter 'x' is never used">x</warning>: Continuation<Any>) {

    }

    <info descr="null">suspend</info> fun String.memberExtension(<warning descr="[UNUSED_PARAMETER] Parameter 'x' is never used">x</warning>: Continuation<Int>) {

    }

    <error descr="[INAPPLICABLE_MODIFIER] 'suspend' modifier is inapplicable. The reason is that return type of suspension function must be a kotlin.Unit, but Int was found"><info descr="null">suspend</info></error> fun returnsNotUnit(<warning descr="[UNUSED_PARAMETER] Parameter 'x' is never used">x</warning>: Continuation<Int>) = 1
}
