// LL_FIR_DIVERGENCE
// In LLFir mode of diagnostic tests, Fir2IR stage is deliberately not run. So no Fir2IR diagnostics may be in .ll.kt files
// LL_FIR_DIVERGENCE
package test

// const val prop1: null
const val prop1 = <!DIVISION_BY_ZERO!>1 / 0<!>

// const val prop2: Infinity.toDouble()
const val prop2 = <!DIVISION_BY_ZERO!>1 / 0.0<!>

// const val prop3: Infinity.toDouble()
const val prop3 = <!DIVISION_BY_ZERO!>1.0 / 0<!>

// const val prop4: 10.0.toDouble()
const val prop4 = 1 / 0.1

// const val prop5: null
const val prop5 = 1 / 0.toLong()

// const val prop6: Infinity.toDouble()
const val prop6 = 1.0 / 0.toInt()

// const val prop7: Infinity.toDouble()
const val prop7 = 1.0 / 0.toLong()

// const val prop8: Infinity.toDouble()
const val prop8 = 1.0 / 0.toByte()

// const val prop9: Infinity.toDouble()
const val prop9 = 1.0 / 0.toShort()

// const val prop10: Infinity.toDouble()
const val prop10 = 1.0 / 0.toFloat()

// const val prop11: Infinity.toDouble()
const val prop11 = 1.0 / 0.toDouble()

// const val prop12: -Infinity.toDouble()
const val prop12 = <!DIVISION_BY_ZERO!>-1.0 / 0<!>

// const val prop13: Infinity.toFloat()
const val prop13 = <!DIVISION_BY_ZERO!>1f / 0<!>

// const val prop14: -Infinity.toFloat()
const val prop14 = <!DIVISION_BY_ZERO!>-1f / 0<!>

// const val prop15: NaN.toDouble()
const val prop15 = <!DIVISION_BY_ZERO!>0.0 / 0<!>

// const val prop16: NaN.toFloat()
const val prop16 = <!DIVISION_BY_ZERO!>0f / 0<!>

// const val prop17: NaN.toDouble()
const val prop17 = <!DIVISION_BY_ZERO!>-0.0 / 0<!>

// const val prop18: NaN.toDouble()
const val prop18 = <!DIVISION_BY_ZERO!>1.0 / 0<!> - <!DIVISION_BY_ZERO!>1.0 / 0<!>

// const val prop19: NaN.toFloat()
const val prop19 = <!DIVISION_BY_ZERO!>1f / 0<!> - <!DIVISION_BY_ZERO!>1f / 0<!>

// const val prop20: NaN.toDouble()
const val prop20 = 1.0 % 0

// const val prop21: NaN.toDouble()
const val prop21 = 0.0 % 0

// const val prop22: NaN.toFloat()
const val prop22 = 1f % 0

// const val prop23: NaN.toDouble()
const val prop23 = -1.0 % 0

// const val prop24: NaN.toDouble()
const val prop24 = -0.0 % 0

// const val prop26: NaN.toDouble()
const val prop26 = 1.0.rem(0)

// const val prop27: Infinity.toDouble()
const val prop27 = <!DIVISION_BY_ZERO!>1.0.div(0)<!>
