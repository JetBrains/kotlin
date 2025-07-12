// DIAGNOSTICS: -UNUSED_PARAMETER

package kotlin.native.internal

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class Escapes(val value: Int)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class PointsTo(vararg val value: Int)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class TypedIntrinsic(val kind: String)

// DFG-handled functions - annotations should be unused
<!UNUSED_ESCAPES_ANNOTATION("function handled manually in DFGBuilder")!>@Escapes(0b01)<!>
external fun createUninitializedInstance(): Any

<!UNUSED_ESCAPES_NOTHING_ANNOTATION("function handled manually in DFGBuilder")!>@Escapes.Nothing<!>
external fun createUninitializedArray(): Any

<!UNUSED_POINTS_TO_ANNOTATION("function handled manually in DFGBuilder")!>@PointsTo(0x01)<!>
external fun createEmptyString(): String

<!UNUSED_ESCAPES_ANNOTATION("function handled manually in DFGBuilder")!>@Escapes(0b01)<!>
external fun reinterpret(x: Any): Any

<!UNUSED_ESCAPES_ANNOTATION("function handled manually in DFGBuilder")!>@Escapes(0b01)<!>
external fun initInstance(x: Any): Any

// No error for DFG functions without annotations
external fun createUninitializedInstanceNoAnnotation(): Any

// Lowered intrinsics - annotations should be unused
@TypedIntrinsic("ATOMIC_GET_FIELD")
<!UNUSED_ESCAPES_ANNOTATION("function is lowered in the compiler")!>@Escapes(0b01)<!>
external fun atomicGetField(x: Any): Any

@TypedIntrinsic("GET_CONTINUATION")
<!UNUSED_ESCAPES_NOTHING_ANNOTATION("function is lowered in the compiler")!>@Escapes.Nothing<!>
external fun getContinuation(): Any

@TypedIntrinsic("ENUM_VALUES")
<!UNUSED_POINTS_TO_ANNOTATION("function is lowered in the compiler")!>@PointsTo(0x01)<!>
external fun enumValues(): Any

@TypedIntrinsic("INTEROP_BITS_TO_FLOAT")
<!UNUSED_ESCAPES_ANNOTATION("function is lowered in the compiler")!>@Escapes(0b01)<!>
external fun bitsToFloat(x: Int): Float

@TypedIntrinsic("WORKER_EXECUTE")
<!UNUSED_ESCAPES_ANNOTATION("function is lowered in the compiler")!>@Escapes(0b01)<!>
external fun workerExecute(x: Any): Any

// No error for intrinsics without annotations
@TypedIntrinsic("ATOMIC_SET_FIELD")
external fun atomicSetFieldNoAnnotation(x: Any, y: Any): Unit

// Non-lowered intrinsic (should still require annotations)
@TypedIntrinsic("PLUS")
@Escapes(0b01)
external fun plusIntrinsic(x: Any): Any

@TypedIntrinsic("REINTERPRET")
<!MISSING_ESCAPE_ANALYSIS_ANNOTATION!>external fun reinterpretIntrinsicNoAnnotation(x: Any): Any<!>

// Invalid intrinsic type doesn't affect checking
@TypedIntrinsic("INVALID_INTRINSIC_TYPE")
@Escapes(0b01)
external fun invalidIntrinsicType(x: Any): Any

// Functions with similar names but different packages should still be checked
package kotlin.collections

<!MISSING_ESCAPE_ANALYSIS_ANNOTATION!>external fun createUninitializedInstance(): Any<!>

@Escapes(0b01)
external fun reinterpret(x: Any): Any