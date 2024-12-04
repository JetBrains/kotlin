@CompileTimeCalculation fun getUnitImplicit(): Unit {}
@CompileTimeCalculation fun getUnitExplicit(): Unit { return Unit }
@CompileTimeCalculation fun getUnitImplicitFromExpression(): Unit { if (true) {} else Unit }
@CompileTimeCalculation fun getUnitImplicitFromTry1(): Unit { try {} finally { 5 } }
@CompileTimeCalculation fun getUnitImplicitFromTry2(): Unit { try {} finally { } }

const val unit1 = <!EVALUATED: `kotlin.Unit`!>Unit.toString()<!>
const val unit2 = <!EVALUATED: `kotlin.Unit`!>getUnitImplicit().toString()<!>
const val unit3 = <!EVALUATED: `kotlin.Unit`!>getUnitExplicit().toString()<!>
const val unit4 = <!EVALUATED: `kotlin.Unit`!>getUnitImplicitFromExpression().toString()<!>
const val unit5 = <!EVALUATED: `kotlin.Unit`!>getUnitImplicitFromTry1().toString()<!>
const val unit6 = <!EVALUATED: `kotlin.Unit`!>getUnitImplicitFromTry2().toString()<!>
