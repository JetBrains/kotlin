fun testFun() {}
val testSimpleVal = 1
val testValWithGetter: Int get() = 42
var testSimpleVar = 2
var testVarWithAccessors: Int
    get() = 42
    set(v) {}

// 1 IrFunction public fun testFun
// 1 IrProperty public val testSimpleVal
// 1 IrProperty public val testValWithGetter
// 2 IrPropertyGetter
// 1 IrProperty public var testSimpleVar
// 1 IrProperty public var testVarWithAccessors
// 1 IrPropertyGetter

// IR_FILE_TXT smoke.txt