// DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE

package kotlin.native.internal

import kotlin.native.internal.*

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class PointsTo(vararg val value: Int)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class Escapes(val value: Int)

// Valid PointsTo annotations
@PointsTo(0x10)
external fun validPointsTo1(x: Any): Any

@PointsTo(0x10, 0x01)
external fun validPointsTo2(x: Any, y: Any): Any

// Invalid PointsTo value
<!INVALID_POINTS_TO_VALUE("null cannot be cast to non-null type kotlin.Int")!>@PointsTo()<!>
external fun invalidPointsToEmpty(x: Any): Any

// PointsTo with kind 1 only allowed for return
<!POINTS_TO_KIND_1_ONLY_FOR_RETURN("x", "y")!>@PointsTo(0x11)<!>
external fun invalidKind1(x: Any, y: Any): Any

// Valid kind 1 for return
@PointsTo(0x01)
external fun validKind1Return(x: Any): Any

// PointsTo from non-escaping type
<!POINTS_TO_FROM_NON_ESCAPING_TYPE("x", "<return>")!>@PointsTo(0x10)<!>
external fun pointsToFromPrimitive(x: Int): Any

// PointsTo to non-escaping type  
<!POINTS_TO_TO_NON_ESCAPING_TYPE("<return>", "x")!>@PointsTo(0x01)<!>
external fun pointsToPrimitive(x: Any): Int

// Complex PointsTo with multiple errors
<!POINTS_TO_FROM_NON_ESCAPING_TYPE("x", "y")!>@PointsTo(0x20, 0x12)<!>
external fun complexPointsTo(x: Int, y: Any, z: Any): Any

// PointsTo with receiver
@PointsTo(0x10)
external fun Any.pointsToWithReceiver(x: Any): Any

<!POINTS_TO_FROM_NON_ESCAPING_TYPE("<receiver>", "x")!>@PointsTo(0x10)<!>
external fun Int.pointsToFromPrimitiveReceiver(x: Any): Any

// Invalid PointsTo index
<!INVALID_POINTS_TO_INDEX(0, 5, "value 1048576 too large for signature of size 2")!>@PointsTo(0x100000)<!>
external fun invalidPointsToIndex(x: Any): Any

// Combination with Escapes
@Escapes(0b11)
@PointsTo(0x10)
external fun combinedAnnotations(x: Any): Any