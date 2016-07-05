// !DIAGNOSTICS: -UNUSED_PARAMETER -NOTHING_TO_INLINE
<!INAPPLICABLE_MODIFIER!>suspend<!> fun notMember(x: Continuation<Int>) {

}

<!INAPPLICABLE_MODIFIER!>suspend<!> fun String.wrongExtension(x: Continuation<Int>) {
}

suspend fun Controller.correctExtension(x: Continuation<Int>) {
}

@AllowSuspendExtensions
class Controller {
    suspend fun valid(x: Continuation<Int>) {

    }

    inline suspend fun inlineFun(x: Continuation<Int>) {

    }

    <!INAPPLICABLE_MODIFIER!>suspend<!> fun noParams() {

    }

    <!INAPPLICABLE_MODIFIER!>suspend<!> fun wrongParam(x: Collection<Int>) {

    }

    <!INAPPLICABLE_MODIFIER!>suspend<!> fun starProjection(vararg x: Continuation<Any>) {

    }

    suspend fun String.memberExtension(x: Continuation<Int>) {

    }
}
