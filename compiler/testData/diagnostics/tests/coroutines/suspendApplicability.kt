// !DIAGNOSTICS: -UNUSED_PARAMETER -NOTHING_TO_INLINE
<!WRONG_MODIFIER_TARGET!>suspend<!> fun notMember(x: Continuation<Int>) {

}

class Controller {
    suspend fun valid(x: Continuation<Int>) {

    }

    inline <!INAPPLICABLE_MODIFIER!>suspend<!> fun inlineFun(x: Continuation<Int>) {

    }

    <!INAPPLICABLE_MODIFIER!>suspend<!> fun noParams() {

    }

    <!INAPPLICABLE_MODIFIER!>suspend<!> fun wrongParam(x: Collection<Int>) {

    }

    <!INAPPLICABLE_MODIFIER!>suspend<!> fun starProjection(vararg x: Continuation<Any>) {

    }
}
