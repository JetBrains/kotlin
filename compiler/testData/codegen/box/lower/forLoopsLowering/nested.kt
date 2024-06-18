// WITH_STDLIB
// FILECHECK_STAGE: CStubs
import kotlin.test.*

// CHECK-LABEL: define i32 @"kfun:#testIntSumOfIntRange(){}kotlin.Int
// CHECK-NOT: iterator
// CHECK-LABEL: epilogue:
inline fun intRange() = (0..10)

fun testIntSumOfIntRange(): Int {
    return intRange().sumOf { it }
}

// CHECK-LABEL: define i32 @"kfun:#testIntSumOfIntRangeAsReturnableBlock(){}kotlin.Int
// CHECK-NOT: iterator
// CHECK-LABEL: epilogue:
inline fun intRangeAsReturnableBlock(): IntRange {
    return 0..10
}

fun testIntSumOfIntRangeAsReturnableBlock(): Int {
    return intRangeAsReturnableBlock().sumOf { it }
}

// Functions using this one, must contain `iterator`, since the actual return type is not IntRange but Iterable,
// so in ForLoopsLowering, optimization to remove iterator cannot be performed
inline fun multipleReturnsRequireDeeperAnalysis(useShorterRange: Boolean): Iterable<Int> {
    if (useShorterRange)
        return listOf(1, 2, 3)
    return 0..10
}

// CHECK-LABEL: define i32 @"kfun:#testMultipleReturnsRequireDeeperAnalysisLongRange(){}kotlin.Int
// CHECK: iterator
// CHECK-LABEL: epilogue:
fun testMultipleReturnsRequireDeeperAnalysisLongRange(): Int {
    return multipleReturnsRequireDeeperAnalysis(useShorterRange = false).sumOf { it }
}

// CHECK-LABEL: define i32 @"kfun:#testMultipleReturnsRequireDeeperAnalysisShortRange(){}kotlin.Int
// CHECK: iterator
// CHECK-LABEL: epilogue:
fun testMultipleReturnsRequireDeeperAnalysisShortRange(): Int {
    return multipleReturnsRequireDeeperAnalysis(useShorterRange = true).sumOf { it }
}

// TODO: complex analysis of IrReturnableBlock is required to find out actual return type is IntRange.
// Meanwhile, iterator is used in every fun which invokes `iterableInt()`
inline fun iterableInt(): Iterable<Int> = (0..10)

// CHECK-LABEL: define i32 @"kfun:#testIntSumOfIterableInt(){}kotlin.Int
// CHECK: iterator
// CHECK-LABEL: epilogue:
fun testIntSumOfIterableInt(): Int {
    return iterableInt().sumOf { it }
}

// CHECK-LABEL: define i32 @"kfun:#testIntSumOfTempVal(){}kotlin.Int
// CHECK: iterator
// CHECK-LABEL: epilogue:
fun testIntSumOfTempVal(): Int {
    val iterableInt: Iterable<Int> = iterableInt()
    return iterableInt.sumOf { it }
}

// TODO: complex analysis of IrReturnableBlock is required to find out actual return type is IntRange.
// Meanwhile, iterator is used in every fun which invokes `iterableIntWithTempVal()`
inline fun iterableIntWithTempVal(): Iterable<Int> {
    val iterableInt: Iterable<Int> = 0..10
    return iterableInt
}

// CHECK-LABEL: define i32 @"kfun:#testIntSumOfIterableIntWithTempVal(){}kotlin.Int
// CHECK: iterator
// CHECK-LABEL: epilogue:
fun testIntSumOfIterableIntWithTempVal(): Int {
    return iterableIntWithTempVal().sumOf { it }
}

// TODO: complex analysis of IrReturnableBlock is required to find out actual return type is IntRange.
// Meanwhile, iterator is used in every fun which invokes `iterableIntWithTempValIterableInt()`
inline fun iterableIntWithTempValIterableInt(): Iterable<Int> {
    val iterableInt: Iterable<Int> = iterableInt()
    return iterableInt
}

// CHECK-LABEL: define i32 @"kfun:#testIntSumOfIterableIntWithTempValIterableInt(){}kotlin.Int
// CHECK: iterator
// CHECK-LABEL: epilogue:
fun testIntSumOfIterableIntWithTempValIterableInt(): Int {
    return iterableIntWithTempValIterableInt().sumOf { it }
}

// CHECK-LABEL: define i32 @"kfun:#testIntSumOfIterableIntInParam(kotlin.collections.Iterable<kotlin.Int>){}kotlin.Int
// CHECK: iterator
// CHECK-LABEL: epilogue:
fun testIntSumOfIterableIntInParam(param: Iterable<Int>): Int {
    return param.sumOf { it }
}

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:#box(){}kotlin.String"
fun box(): String {
    assertEquals(55, testIntSumOfIntRange())
    assertEquals(55, testIntSumOfIntRangeAsReturnableBlock())
    assertEquals(55, testMultipleReturnsRequireDeeperAnalysisLongRange())
    assertEquals(6,  testMultipleReturnsRequireDeeperAnalysisShortRange())
    assertEquals(55, testIntSumOfTempVal())
    assertEquals(55, testIntSumOfIterableInt())
    assertEquals(55, testIntSumOfIterableIntWithTempVal())
    assertEquals(55, testIntSumOfIterableIntWithTempValIterableInt())
    assertEquals(55, testIntSumOfIterableIntInParam(0..10))
    assertEquals(55, testIntSumOfIterableIntInParam(listOf(0, 50, 5)))
    return "OK"
}
