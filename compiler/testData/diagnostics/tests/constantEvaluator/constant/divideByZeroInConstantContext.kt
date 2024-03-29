package test

// const val prop1: null
<!DEBUG_INFO_CONSTANT_VALUE("null")!>const val prop1 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER, DIVISION_BY_ZERO!>1 / 0<!><!>

// const val prop2: Infinity.toDouble()
<!DEBUG_INFO_CONSTANT_VALUE("Infinity.toDouble()")!>const val prop2 = <!DIVISION_BY_ZERO!>1 / 0.0<!><!>

// const val prop3: Infinity.toDouble()
<!DEBUG_INFO_CONSTANT_VALUE("Infinity.toDouble()")!>const val prop3 = <!DIVISION_BY_ZERO!>1.0 / 0<!><!>

// const val prop4: 10.0.toDouble()
<!DEBUG_INFO_CONSTANT_VALUE("10.0.toDouble()")!>const val prop4 = 1 / 0.1<!>

// const val prop5: null
<!DEBUG_INFO_CONSTANT_VALUE("null")!>const val prop5 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER, DIVISION_BY_ZERO!>1 / 0.toLong()<!><!>

// const val prop6: Infinity.toDouble()
<!DEBUG_INFO_CONSTANT_VALUE("Infinity.toDouble()")!>const val prop6 = <!DIVISION_BY_ZERO!>1.0 / 0.toInt()<!><!>

// const val prop7: Infinity.toDouble()
<!DEBUG_INFO_CONSTANT_VALUE("Infinity.toDouble()")!>const val prop7 = <!DIVISION_BY_ZERO!>1.0 / 0.toLong()<!><!>

// const val prop8: Infinity.toDouble()
<!DEBUG_INFO_CONSTANT_VALUE("Infinity.toDouble()")!>const val prop8 = <!DIVISION_BY_ZERO!>1.0 / 0.toByte()<!><!>

// const val prop9: Infinity.toDouble()
<!DEBUG_INFO_CONSTANT_VALUE("Infinity.toDouble()")!>const val prop9 = <!DIVISION_BY_ZERO!>1.0 / 0.toShort()<!><!>

// const val prop10: Infinity.toDouble()
<!DEBUG_INFO_CONSTANT_VALUE("Infinity.toDouble()")!>const val prop10 = <!DIVISION_BY_ZERO!>1.0 / 0.toFloat()<!><!>

// const val prop11: Infinity.toDouble()
<!DEBUG_INFO_CONSTANT_VALUE("Infinity.toDouble()")!>const val prop11 = <!DIVISION_BY_ZERO!>1.0 / 0.toDouble()<!><!>

// const val prop12: -Infinity.toDouble()
<!DEBUG_INFO_CONSTANT_VALUE("-Infinity.toDouble()")!>const val prop12 = <!DIVISION_BY_ZERO!>-1.0 / 0<!><!>

// const val prop13: Infinity.toFloat()
<!DEBUG_INFO_CONSTANT_VALUE("Infinity.toFloat()")!>const val prop13 = <!DIVISION_BY_ZERO!>1f / 0<!><!>

// const val prop14: -Infinity.toFloat()
<!DEBUG_INFO_CONSTANT_VALUE("-Infinity.toFloat()")!>const val prop14 = <!DIVISION_BY_ZERO!>-1f / 0<!><!>

// const val prop15: NaN.toDouble()
<!DEBUG_INFO_CONSTANT_VALUE("NaN.toDouble()")!>const val prop15 = <!DIVISION_BY_ZERO!>0.0 / 0<!><!>

// const val prop16: NaN.toFloat()
<!DEBUG_INFO_CONSTANT_VALUE("NaN.toFloat()")!>const val prop16 = <!DIVISION_BY_ZERO!>0f / 0<!><!>

// const val prop17: NaN.toDouble()
<!DEBUG_INFO_CONSTANT_VALUE("NaN.toDouble()")!>const val prop17 = <!DIVISION_BY_ZERO!>-0.0 / 0<!><!>

// const val prop18: NaN.toDouble()
<!DEBUG_INFO_CONSTANT_VALUE("NaN.toDouble()")!>const val prop18 = <!DIVISION_BY_ZERO!>1.0 / 0<!> - <!DIVISION_BY_ZERO!>1.0 / 0<!><!>

// const val prop19: NaN.toFloat()
<!DEBUG_INFO_CONSTANT_VALUE("NaN.toFloat()")!>const val prop19 = <!DIVISION_BY_ZERO!>1f / 0<!> - <!DIVISION_BY_ZERO!>1f / 0<!><!>

// const val prop20: NaN.toDouble()
<!DEBUG_INFO_CONSTANT_VALUE("NaN.toDouble()")!>const val prop20 = <!DIVISION_BY_ZERO!>1.0 % 0<!><!>

// const val prop21: NaN.toDouble()
<!DEBUG_INFO_CONSTANT_VALUE("NaN.toDouble()")!>const val prop21 = <!DIVISION_BY_ZERO!>0.0 % 0<!><!>

// const val prop22: NaN.toFloat()
<!DEBUG_INFO_CONSTANT_VALUE("NaN.toFloat()")!>const val prop22 = <!DIVISION_BY_ZERO!>1f % 0<!><!>

// const val prop23: NaN.toDouble()
<!DEBUG_INFO_CONSTANT_VALUE("NaN.toDouble()")!>const val prop23 = <!DIVISION_BY_ZERO!>-1.0 % 0<!><!>

// const val prop24: NaN.toDouble()
<!DEBUG_INFO_CONSTANT_VALUE("NaN.toDouble()")!>const val prop24 = <!DIVISION_BY_ZERO!>-0.0 % 0<!><!>

// const val prop26: NaN.toDouble()
<!DEBUG_INFO_CONSTANT_VALUE("NaN.toDouble()")!>const val prop26 = <!DIVISION_BY_ZERO!>1.0.rem(0)<!><!>

// const val prop27: Infinity.toDouble()
<!DEBUG_INFO_CONSTANT_VALUE("Infinity.toDouble()")!>const val prop27 = <!DIVISION_BY_ZERO!>1.0.div(0)<!><!>
