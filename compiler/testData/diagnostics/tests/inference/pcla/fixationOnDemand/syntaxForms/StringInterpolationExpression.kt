// FIR_IDENTICAL

fun test() {
    val resultA = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner())
        // fixation of OTv is not required
        "pad ${otvOwner.provide()} pad"
        // expected: Interloper <: OTv
        otvOwner.constrain(Interloper)
    }
    // expected: CST(ScopeOwner, Interloper) == BaseType
    <!DEBUG_INFO_EXPRESSION_TYPE("BaseType")!>resultA<!>

    val resultB = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner())
        // fixation of OTv is not required
        """pad
        ${otvOwner.provide()}
        pad"""
        // expected: Interloper <: OTv
        otvOwner.constrain(Interloper)
    }
    // expected: CST(ScopeOwner, Interloper) == BaseType
    <!DEBUG_INFO_EXPRESSION_TYPE("BaseType")!>resultB<!>
}


class TypeVariableOwner<T> {
    fun constrain(subtypeValue: T) {}
    fun provide(): T = null!!
}

fun <OT> pcla(lambda: (TypeVariableOwner<OT>) -> Unit): OT = null!!

interface BaseType

class ScopeOwner: BaseType {
    override fun toString(): String = "ScopeOwner"
}

object Interloper: BaseType
