package test

// val prop1: null
<!DEBUG_INFO_CONSTANT_VALUE("null")!>val prop1 = <!DIVISION_BY_ZERO!>1 / 0<!><!>

// val prop2: Infinity.toDouble()
<!DEBUG_INFO_CONSTANT_VALUE("Infinity.toDouble()")!>val prop2 = <!DIVISION_BY_ZERO!>1 / 0.0<!><!>

// val prop3: Infinity.toDouble()
<!DEBUG_INFO_CONSTANT_VALUE("Infinity.toDouble()")!>val prop3 = <!DIVISION_BY_ZERO!>1.0 / 0<!><!>

// val prop4: 10.0.toDouble()
<!DEBUG_INFO_CONSTANT_VALUE("10.0.toDouble()")!>val prop4 = 1 / 0.1<!>

// val prop5: null
<!DEBUG_INFO_CONSTANT_VALUE("null")!>val prop5 = <!DIVISION_BY_ZERO!>1 / 0.toLong()<!><!>

// val prop6: Infinity.toDouble()
<!DEBUG_INFO_CONSTANT_VALUE("Infinity.toDouble()")!>val prop6 = <!DIVISION_BY_ZERO!>1.0 / 0.toInt()<!><!>

// val prop7: Infinity.toDouble()
<!DEBUG_INFO_CONSTANT_VALUE("Infinity.toDouble()")!>val prop7 = <!DIVISION_BY_ZERO!>1.0 / 0.toLong()<!><!>

// val prop8: Infinity.toDouble()
<!DEBUG_INFO_CONSTANT_VALUE("Infinity.toDouble()")!>val prop8 = <!DIVISION_BY_ZERO!>1.0 / 0.toByte()<!><!>

// val prop9: Infinity.toDouble()
<!DEBUG_INFO_CONSTANT_VALUE("Infinity.toDouble()")!>val prop9 = <!DIVISION_BY_ZERO!>1.0 / 0.toShort()<!><!>

// val prop10: Infinity.toDouble()
<!DEBUG_INFO_CONSTANT_VALUE("Infinity.toDouble()")!>val prop10 = <!DIVISION_BY_ZERO!>1.0 / 0.toFloat()<!><!>

// val prop11: Infinity.toDouble()
<!DEBUG_INFO_CONSTANT_VALUE("Infinity.toDouble()")!>val prop11 = <!DIVISION_BY_ZERO!>1.0 / 0.toDouble()<!><!>

// val prop12: -Infinity.toDouble()
<!DEBUG_INFO_CONSTANT_VALUE("-Infinity.toDouble()")!>val prop12 = <!DIVISION_BY_ZERO!>-1.0 / 0<!><!>

// val prop13: Infinity.toFloat()
<!DEBUG_INFO_CONSTANT_VALUE("Infinity.toFloat()")!>val prop13 = <!DIVISION_BY_ZERO!>1f / 0<!><!>

// val prop14: -Infinity.toFloat()
<!DEBUG_INFO_CONSTANT_VALUE("-Infinity.toFloat()")!>val prop14 = <!DIVISION_BY_ZERO!>-1f / 0<!><!>

// val prop15: NaN.toDouble()
<!DEBUG_INFO_CONSTANT_VALUE("NaN.toDouble()")!>val prop15 = <!DIVISION_BY_ZERO!>0.0 / 0<!><!>

// val prop16: NaN.toFloat()
<!DEBUG_INFO_CONSTANT_VALUE("NaN.toFloat()")!>val prop16 = <!DIVISION_BY_ZERO!>0f / 0<!><!>

// val prop17: NaN.toDouble()
<!DEBUG_INFO_CONSTANT_VALUE("NaN.toDouble()")!>val prop17 = <!DIVISION_BY_ZERO!>-0.0 / 0<!><!>

// val prop18: NaN.toDouble()
<!DEBUG_INFO_CONSTANT_VALUE("NaN.toDouble()")!>val prop18 = <!DIVISION_BY_ZERO!>1.0 / 0<!> - <!DIVISION_BY_ZERO!>1.0 / 0<!><!>

// val prop19: NaN.toFloat()
<!DEBUG_INFO_CONSTANT_VALUE("NaN.toFloat()")!>val prop19 = <!DIVISION_BY_ZERO!>1f / 0<!> - <!DIVISION_BY_ZERO!>1f / 0<!><!>

// val prop20: NaN.toDouble()
<!DEBUG_INFO_CONSTANT_VALUE("NaN.toDouble()")!>val prop20 = <!DIVISION_BY_ZERO!>1.0 % 0<!><!>

// val prop21: NaN.toDouble()
<!DEBUG_INFO_CONSTANT_VALUE("NaN.toDouble()")!>val prop21 = <!DIVISION_BY_ZERO!>0.0 % 0<!><!>

// val prop22: NaN.toFloat()
<!DEBUG_INFO_CONSTANT_VALUE("NaN.toFloat()")!>val prop22 = <!DIVISION_BY_ZERO!>1f % 0<!><!>

// val prop23: NaN.toDouble()
<!DEBUG_INFO_CONSTANT_VALUE("NaN.toDouble()")!>val prop23 = <!DIVISION_BY_ZERO!>-1.0 % 0<!><!>

// val prop24: NaN.toDouble()
<!DEBUG_INFO_CONSTANT_VALUE("NaN.toDouble()")!>val prop24 = <!DIVISION_BY_ZERO!>-0.0 % 0<!><!>

// val prop26: NaN.toDouble()
<!DEBUG_INFO_CONSTANT_VALUE("NaN.toDouble()")!>val prop26 = <!DIVISION_BY_ZERO!>1.0.rem(0)<!><!>

// val prop27: Infinity.toDouble()
<!DEBUG_INFO_CONSTANT_VALUE("Infinity.toDouble()")!>val prop27 = <!DIVISION_BY_ZERO!>1.0.div(0)<!><!>
