fun test() {
    pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner())
        val result = otvOwner.provide().nullaryFunction()
        <!DEBUG_INFO_EXPRESSION_TYPE("MemberFunctionResult")!>result<!>
    }
    pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner())
        val result = otvOwner.provide().unaryFunction(SpecificCallArgument())
        <!DEBUG_INFO_EXPRESSION_TYPE("MemberFunctionResult")!>result<!>
    }
    pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner())
        val result = otvOwner.provide().unaryFunction(GeneralCallArgument())
        <!DEBUG_INFO_EXPRESSION_TYPE("ExtensionFunctionResult")!>result<!>
    }
}


class TypeVariableOwner<T> {
    fun constrain(subtypeValue: T) {}
    fun provide(): T = null!!
}

fun <OT> pcla(lambda: (TypeVariableOwner<OT>) -> Unit): OT = null!!

open class GeneralCallArgument
class SpecificCallArgument: GeneralCallArgument()

object MemberFunctionResult
object ExtensionFunctionResult

class ScopeOwner {
    fun nullaryFunction(): MemberFunctionResult = MemberFunctionResult
    fun unaryFunction(arg: SpecificCallArgument): MemberFunctionResult = MemberFunctionResult
}

fun ScopeOwner.<!EXTENSION_SHADOWED_BY_MEMBER!>nullaryFunction<!>(): ExtensionFunctionResult = ExtensionFunctionResult
fun ScopeOwner.unaryFunction(arg: GeneralCallArgument): ExtensionFunctionResult = ExtensionFunctionResult
