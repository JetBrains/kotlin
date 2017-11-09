package test

// val prop1: 3.4028235E38.toFloat()
val prop1: Float = java.lang.Float.MAX_VALUE + 1

// val prop2: 3.4028234663852886E38.toDouble()
val prop2: Double = java.lang.Float.MAX_VALUE + 1.0

// val prop3: 3.4028235E38.toFloat()
val prop3 = java.lang.Float.MAX_VALUE + 1

// val prop4: 3.4028235E38.toFloat()
val prop4 = java.lang.Float.MAX_VALUE - 1

// val prop5: 3.4028235E38.toFloat()
val prop5: Int = java.lang.Float.MAX_VALUE + 1

// val prop6: 2.0.toFloat()
val prop6: Float = 1.0.toFloat() + 1

// val prop7: 2.0.toDouble()
val prop7: Double = 1.0 + 1.0

// val prop8: 2.0.toDouble()
val prop8: Float = 1.0.toDouble() + 1.0

// val prop9: -2.0.toDouble()
val prop9: Double = -2.0

// val prop10: Infinity.toFloat()
val prop10: Float = 1.2E400F

// val prop11: 0.0.toFloat()
val prop11: Float = 1.2E-400F

// val prop12: Infinity.toFloat()
val prop12: Float = 11111111111111111111111111111111111111111111111111111111111111111F

// val prop13: 0.0.toFloat()
val prop13: Float = 0.000000000000000000000000000000000000000000000000000000000000001F

// val prop14: 1.0E-39.toFloat()
val prop14: Float = 0.000000000000000000000000000000000000001000000000000000000000000F

// val prop15: Infinity.toDouble()
val prop15: Double = 1.2E400

// val prop16: 0.0.toDouble()
val prop16: Double = 1.2E-400
