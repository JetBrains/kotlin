fun testFun(): String { return "OK" }
val testSimpleVal = 1
val testValWithGetter: Int get() = 42
var testSimpleVar = 2
var testVarWithAccessors: Int
    get() = 42
    set(v) {}

// 1 FUN public fun testFun
// 1 PROPERTY public val testSimpleVal
// 2 PROPERTY_GETTER
// 1 PROPERTY_SETTER
