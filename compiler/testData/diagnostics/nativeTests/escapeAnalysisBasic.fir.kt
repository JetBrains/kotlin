// DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE

// MODULE: escape

package kotlin.native.internal.escapeAnalysis

// Annotations for testing
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class Escapes(val value: Int)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class PointsTo(vararg val value: Int)

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class HasFinalizer

// MODULE: main(escape)
package kotlin.native.internal

import kotlin.native.internal.escapeAnalysis.*

// Valid external functions with escape annotations
@Escapes(0b01)
external fun externalFun1(x: Any): Any

@Escapes(0b11)
external fun externalFun2(x: Any, y: Any): Unit

@Escapes.Nothing
external fun externalFunNothing(x: Any): Any

@PointsTo(0, 1)
external fun externalFunPointsTo(x: Any): Any

// Non-external functions - annotations should be unused
<!UNUSED_ESCAPES_ANNOTATION("non-external function")!>@Escapes(0b01)<!>
fun regularFun1(x: Any): Any = x

<!UNUSED_ESCAPES_NOTHING_ANNOTATION("non-external function")!>@Escapes.Nothing<!>
fun regularFun2(x: Any): Any = x

<!UNUSED_POINTS_TO_ANNOTATION("non-external function")!>@PointsTo(0, 1)<!>
fun regularFun3(x: Any): Any = x

// External function without required annotations
<!MISSING_ESCAPE_ANALYSIS_ANNOTATION!>external fun externalFunNoAnnotation(x: Any): Any<!>

// Conflicting annotations
<!CONFLICTING_ESCAPES_AND_ESCAPES_NOTHING!>@Escapes(0b01)
@Escapes.Nothing
external fun externalFunConflicting(x: Any): Any<!>

// External functions with primitive types that cannot escape
<!UNUSED_ESCAPES_ANNOTATION("all of function parameters, receivers and the return value types cannot escape to the heap")!>@Escapes(0b111)<!>
external fun externalPrimitives(x: Int, y: Boolean): Double

<!UNUSED_ESCAPES_NOTHING_ANNOTATION("all of function parameters, receivers and the return value types cannot escape to the heap")!>@Escapes.Nothing<!>
external fun externalUnit(x: Int): Unit

<!UNUSED_POINTS_TO_ANNOTATION("all of function parameters, receivers and the return value types cannot escape to the heap")!>@PointsTo(0, 1)<!>
external fun externalNothing(x: Int): Nothing

// Class with HasFinalizer
@HasFinalizer
class FinalizableClass

// External function with must-escape type but no @Escapes
<!MISSING_ESCAPES_FOR_MUST_ESCAPE_TYPE!>external fun externalWithFinalizer(x: FinalizableClass): Any<!>

// Valid use with finalizer
@Escapes(0b01)
external fun externalWithFinalizerOk(x: FinalizableClass): Any

// Invalid escapes value
<!INVALID_ESCAPES_VALUE("0 must not be negative and not have bits higher than 2")!>@Escapes(-1)<!>
external fun externalInvalidEscapes(x: Any): Any

// Escapes marked on non-escaping type
<!ESCAPES_MARKED_ON_NON_ESCAPING_TYPE("x")!>@Escapes(0b01)<!>
external fun externalEscapesOnPrimitive(x: Int): Any

// Escapes not marked on must-escape type
<!ESCAPES_NOT_MARKED_ON_MUST_ESCAPE_TYPE("x")!>@Escapes(0b10)<!>
external fun externalNoEscapeForFinalizer(x: FinalizableClass): Any

// Extension functions
<!UNUSED_ESCAPES_ANNOTATION("all of function parameters, receivers and the return value types cannot escape to the heap")!>@Escapes(0b111)<!>
external fun Int.externalExtension(x: Int): Int

@Escapes(0b001)
external fun Any.externalExtensionAny(x: Int): Any

// Vararg parameters
@Escapes(0b11)
external fun externalVararg(vararg x: Any): Any
