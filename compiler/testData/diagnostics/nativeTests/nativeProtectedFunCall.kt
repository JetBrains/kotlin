// ISSUE: KT-58623

package pack

open class ProtectedInsideInlineParent {
    protected var protectedParentVar = 0
    protected fun protectedParentFun() = 0
}

open class ProtectedInsideInlineError : ProtectedInsideInlineParent() {
    protected var protectedVar = 0
    protected fun protectedFun() = 0

    inline fun publicInlineUserFun() {
        <!UNRESOLVED_REFERENCE!>println<!>(<!PROTECTED_CALL_FROM_PUBLIC_INLINE_ERROR!>protectedVar<!> + <!PROTECTED_CALL_FROM_PUBLIC_INLINE_ERROR!>protectedParentVar<!>)
        <!PROTECTED_CALL_FROM_PUBLIC_INLINE_ERROR!>protectedFun<!>()
        <!PROTECTED_CALL_FROM_PUBLIC_INLINE_ERROR!>protectedParentFun<!>()
    }

    inline var publicInlineUserVal: Int
        get() = <!PROTECTED_CALL_FROM_PUBLIC_INLINE_ERROR!>protectedVar<!> + <!PROTECTED_CALL_FROM_PUBLIC_INLINE_ERROR!>protectedFun<!>() + <!PROTECTED_CALL_FROM_PUBLIC_INLINE_ERROR!>protectedParentVar<!> + <!PROTECTED_CALL_FROM_PUBLIC_INLINE_ERROR!>protectedParentFun<!>()
        set(<!UNUSED_PARAMETER!>value<!>) { <!PROTECTED_CALL_FROM_PUBLIC_INLINE_ERROR!>protectedVar<!> + <!PROTECTED_CALL_FROM_PUBLIC_INLINE_ERROR!>protectedFun<!>() + <!PROTECTED_CALL_FROM_PUBLIC_INLINE_ERROR!>protectedParentVar<!> + <!PROTECTED_CALL_FROM_PUBLIC_INLINE_ERROR!>protectedParentFun<!>() }
}
