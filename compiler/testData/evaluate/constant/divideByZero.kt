package test

// val prop1: null
val prop1 = 1 / 0

// val prop2: Infinity.toDouble()
val prop2 = 1 / 0.0

// val prop3: Infinity.toDouble()
val prop3 = 1.0 / 0

// val prop4: 10.0.toDouble()
val prop4 = 1 / 0.1

// val prop5: null
val prop5 = 1 / 0.toLong()

// val prop6: Infinity.toDouble()
val prop6 = 1.0 / 0.toInt()

// val prop7: Infinity.toDouble()
val prop7 = 1.0 / 0.toLong()

// val prop8: Infinity.toDouble()
val prop8 = 1.0 / 0.toByte()

// val prop9: Infinity.toDouble()
val prop9 = 1.0 / 0.toShort()

// val prop10: Infinity.toDouble()
val prop10 = 1.0 / 0.toFloat()

// val prop11: Infinity.toDouble()
val prop11 = 1.0 / 0.toDouble()

// val prop12: -Infinity.toDouble()
val prop12 = -1.0 / 0

// val prop13: Infinity.toFloat()
val prop13 = 1f / 0

// val prop14: -Infinity.toFloat()
val prop14 = -1f / 0

// val prop15: NaN.toDouble()
val prop15 = 0.0 / 0

// val prop16: NaN.toFloat()
val prop16 = 0f / 0

// val prop17: NaN.toDouble()
val prop17 = -0.0 / 0

// val prop18: NaN.toDouble()
val prop18 = 1.0 / 0 - 1.0 / 0

// val prop19: NaN.toFloat()
val prop19 = 1f / 0 - 1f / 0

// val prop20: NaN.toDouble()
val prop20 = 1.0 % 0

// val prop21: NaN.toDouble()
val prop21 = 0.0 % 0

// val prop22: NaN.toFloat()
val prop22 = 1f % 0

// val prop23: NaN.toDouble()
val prop23 = -1.0 % 0

// val prop24: NaN.toDouble()
val prop24 = -0.0 % 0

// val prop25: NaN.toDouble()
val prop25 = 1.0.mod(0)

// val prop26: NaN.toDouble()
val prop26 = 1.0.rem(0)

// val prop27: Infinity.toDouble()
val prop27 = 1.0.div(0)