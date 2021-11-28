@CompileTimeCalculation fun getUnitImplicit(): Unit {}
@CompileTimeCalculation fun getUnitExplicit(): Unit { return Unit }
@CompileTimeCalculation fun getUnitImplicitFromExpression(): Unit { if (true) {} else Unit }
@CompileTimeCalculation fun getUnitImplicitFromTry1(): Unit { try {} finally { 5 } }
@CompileTimeCalculation fun getUnitImplicitFromTry2(): Unit { try {} finally { } }

const val unit1 = Unit.<!EVALUATED: `kotlin.Unit`!>toString()<!>
const val unit2 = getUnitImplicit().<!EVALUATED: `kotlin.Unit`!>toString()<!>
const val unit3 = getUnitExplicit().<!EVALUATED: `kotlin.Unit`!>toString()<!>
const val unit4 = getUnitImplicitFromExpression().<!EVALUATED: `kotlin.Unit`!>toString()<!>
const val unit5 = getUnitImplicitFromTry1().<!EVALUATED: `kotlin.Unit`!>toString()<!>
const val unit6 = getUnitImplicitFromTry2().<!EVALUATED: `kotlin.Unit`!>toString()<!>
