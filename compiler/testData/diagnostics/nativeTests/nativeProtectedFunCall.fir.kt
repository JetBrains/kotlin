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
        <!UNRESOLVED_REFERENCE!>println<!>(protectedVar + protectedParentVar)
        protectedFun()
        protectedParentFun()
    }

    inline var publicInlineUserVal: Int
        get() = protectedVar + protectedFun() + protectedParentVar + protectedParentFun()
        set(value) { protectedVar + protectedFun() + protectedParentVar + protectedParentFun() }
}
