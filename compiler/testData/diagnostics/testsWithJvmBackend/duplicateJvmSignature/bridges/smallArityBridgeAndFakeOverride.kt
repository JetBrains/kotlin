// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER
// LANGUAGE: -ForbidBridgesConflictingWithInheritedMethodsInJvmCode
// ISSUE: KT-13712

typealias SmallFunctionType = Function1<Int, Any>

open class SmallInvoker {
    open fun invoke(arg: Any): Any = 0
}

<!ACCIDENTAL_OVERRIDE_BY_BRIDGE_METHOD_WARNING!>class SmallImpl: SmallInvoker(), SmallFunctionType {
    override fun invoke(p1: Int): Any = 42
    // bridge `invoke(Any): Any` is generated here
}<!>
