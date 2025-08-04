// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER
// LANGUAGE: -ForbidBridgesConflictingWithInheritedMethodsInJvmCode
// ISSUE: KT-13712

typealias BigFunctionType =
    Function25<
        Int, Int, Int, Int, Int,
        Int, Int, Int, Int, Int,
        Int, Int, Int, Int, Int,
        Int, Int, Int, Int, Int,
        Int, Int, Int, Int, Int,
        Any
    >

open class BigInvoker {
    open fun invoke(vararg ps: Any): Any = 0
}

<!ACCIDENTAL_OVERRIDE_BY_BRIDGE_METHOD_WARNING!>class BigImpl: BigInvoker(), BigFunctionType {
    override fun invoke(
        p1: Int, p2: Int, p3: Int, p4: Int, p5: Int,
        p6: Int, p7: Int, p8: Int, p9: Int, p10: Int,
        p11: Int, p12: Int, p13: Int, p14: Int, p15: Int,
        p16: Int, p17: Int, p18: Int, p19: Int, p20: Int,
        p21: Int, p22: Int, p23: Int, p24: Int, p25: Int
    ): Any {
        return p25
    }
}<!>

open class BigInvokerWithArray {
    open fun invoke(ps: Array<Any>): Any = 0
}

<!ACCIDENTAL_OVERRIDE_BY_BRIDGE_METHOD_WARNING!>class BigImplWithArray: BigInvokerWithArray(), BigFunctionType {
    override fun invoke(
        p1: Int, p2: Int, p3: Int, p4: Int, p5: Int,
        p6: Int, p7: Int, p8: Int, p9: Int, p10: Int,
        p11: Int, p12: Int, p13: Int, p14: Int, p15: Int,
        p16: Int, p17: Int, p18: Int, p19: Int, p20: Int,
        p21: Int, p22: Int, p23: Int, p24: Int, p25: Int
    ): Any {
        return p25
    }
}<!>
