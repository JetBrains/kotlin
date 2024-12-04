// CHECK_BYTECODE_LISTING___ // Temporary turn off the directive, revert after the next bootstrapt advance
// WITH_STDLIB
// WITH_COROUTINES
// TARGET_BACKEND: JVM_IR
// LANGUAGE: +ValueClasses
// FIR_IDENTICAL

// FILE: caller.kt
import kotlin.coroutines.*

fun runSuspend(block: suspend () -> Unit) {
    block.startCoroutine(Continuation(EmptyCoroutineContext) { it.getOrThrow() })
}

// FILE: test.kt

@JvmInline
value class DPoint(val x: Double, val y: Double) {
    fun f(z: Double) = x + y + z
    suspend fun suspended() = this
    inline suspend fun suspendedInline() = this
    
    fun functionWithInlineClass(other: DPoint, n: UInt) = x.toUInt() + y.toUInt() + other.x.toUInt() + other.y.toUInt() + n
    
    suspend fun suspendFunctionWithLambda(other: DPoint, f: suspend (DPoint) -> DPoint) = DPoint(f(this).x, f(other).y).suspendedInline()

    suspend fun suspendInlineFunctionWithLambda(other: DPoint, f: suspend (DPoint) -> DPoint) = DPoint(f(this).x, f(other).y).suspendedInline()
}

fun DPoint.extensionFunction(z: Double) = x + y + z

fun g(point: DPoint, z: Double) = point.f(z)

class A(val point: DPoint) {
    fun f(otherDPoint: DPoint, z: Double) = point.f(z) * otherDPoint.f(z)
}

fun consume(point1: DPoint, point2: DPoint, f: (DPoint, DPoint) -> DPoint) = f(point1, point2)

inline fun consumeInline(point1: DPoint, point2: DPoint, f: (DPoint, DPoint) -> DPoint) = f(point1, point2)

operator fun DPoint.plus(other: DPoint) = DPoint(this.x + other.x, this.y + other.y)

fun makeDPoint(x: Double, y: Double, maker: (Double, Double) -> DPoint) = maker(x, y)

inline fun makeDPointInline(x: Double, y: Double, maker: (Double, Double) -> DPoint) = maker(x, y)

inline fun <T> id(x: T) = x

fun box(): String {
    val dPoint = DPoint(1.0, 2.0)
    val a = A(dPoint)

    require((::DPoint)(1.0, 2.0) == dPoint)
    require((dPoint::f)(3.0) == 6.0)
    require((DPoint::f)(dPoint, 3.0) == 6.0)
    require((dPoint::extensionFunction)(3.0) == 6.0)
    require((DPoint::extensionFunction)(dPoint, 3.0) == 6.0)

    runSuspend { require(dPoint.suspended() == dPoint) }
    runSuspend { require((dPoint::suspended)() == dPoint) }
    runSuspend { require((DPoint::suspended)(dPoint) == dPoint) }
    runSuspend { require(dPoint.suspendedInline() == dPoint) }
    runSuspend { require((dPoint::suspendedInline)() == dPoint) }
    runSuspend { require((DPoint::suspendedInline)(dPoint) == dPoint) }

    runSuspend { require(dPoint.suspendFunctionWithLambda(dPoint, DPoint::suspended) == dPoint) }
    runSuspend { require((dPoint::suspendFunctionWithLambda)(dPoint, DPoint::suspended) == dPoint) }
    runSuspend { require((DPoint::suspendFunctionWithLambda)(dPoint, dPoint, DPoint::suspended) == dPoint) }
    runSuspend { require(dPoint.suspendFunctionWithLambda(dPoint, DPoint::suspendedInline) == dPoint) }
    runSuspend { require((dPoint::suspendFunctionWithLambda)(dPoint, DPoint::suspendedInline) == dPoint) }
    runSuspend { require((DPoint::suspendFunctionWithLambda)(dPoint, dPoint, DPoint::suspendedInline) == dPoint) }
    runSuspend { require(dPoint.suspendFunctionWithLambda(dPoint) { it.suspended() } == dPoint) }
    runSuspend { require((dPoint::suspendFunctionWithLambda)(dPoint) { it.suspended() } == dPoint) }
    runSuspend { require((DPoint::suspendFunctionWithLambda)(dPoint, dPoint) { it.suspended() } == dPoint) }
    runSuspend { require(dPoint.suspendFunctionWithLambda(dPoint) { it.suspendedInline() } == dPoint) }
    runSuspend { require((dPoint::suspendFunctionWithLambda)(dPoint) { it.suspendedInline() } == dPoint) }
    runSuspend { require((DPoint::suspendFunctionWithLambda)(dPoint, dPoint) { it.suspendedInline() } == dPoint) }

    runSuspend { require(dPoint.suspendInlineFunctionWithLambda(dPoint, DPoint::suspended) == dPoint) }
    runSuspend { require((dPoint::suspendInlineFunctionWithLambda)(dPoint, DPoint::suspended) == dPoint) }
    runSuspend { require((DPoint::suspendInlineFunctionWithLambda)(dPoint, dPoint, DPoint::suspended) == dPoint) }
    runSuspend { require(dPoint.suspendInlineFunctionWithLambda(dPoint, DPoint::suspendedInline) == dPoint) }
    runSuspend { require((dPoint::suspendInlineFunctionWithLambda)(dPoint, DPoint::suspendedInline) == dPoint) }
    runSuspend { require((DPoint::suspendInlineFunctionWithLambda)(dPoint, dPoint, DPoint::suspendedInline) == dPoint) }
    runSuspend { require(dPoint.suspendInlineFunctionWithLambda(dPoint) { it.suspended() } == dPoint) }
    runSuspend { require((dPoint::suspendInlineFunctionWithLambda)(dPoint) { it.suspended() } == dPoint) }
    runSuspend { require((DPoint::suspendInlineFunctionWithLambda)(dPoint, dPoint) { it.suspended() } == dPoint) }
    runSuspend { require(dPoint.suspendInlineFunctionWithLambda(dPoint) { it.suspendedInline() } == dPoint) }
    runSuspend { require((dPoint::suspendInlineFunctionWithLambda)(dPoint) { it.suspendedInline() } == dPoint) }
    runSuspend { require((DPoint::suspendInlineFunctionWithLambda)(dPoint, dPoint) { it.suspendedInline() } == dPoint) }

    require((dPoint::functionWithInlineClass)(dPoint, 100U) == 106U)
    require((DPoint::functionWithInlineClass)(dPoint, dPoint, 100U) == 106U)

    require((::g)(dPoint, 3.0) == 6.0)
    require((a::f)(dPoint, 3.0) == 36.0)
    require((A::f)(a, dPoint, 3.0) == 36.0)

    require((::DPoint)(1.0, DPoint(1.0, 2.0).y) == dPoint)
    require((dPoint::f)(DPoint(1.0, 3.0).y) == 6.0)
    require((DPoint::f)(dPoint, DPoint(1.0, 3.0).y) == 6.0)
    require((::g)(dPoint, DPoint(1.0, 3.0).y) == 6.0)
    require((a::f)(dPoint, DPoint(1.0, 3.0).y) == 36.0)
    require((A::f)(a, dPoint, DPoint(1.0, 3.0).y) == 36.0)

    require(consume(DPoint(1.0, 2.0), DPoint(3.0, 4.0), DPoint::plus) == DPoint(4.0, 6.0))
    require(consume(DPoint(1.0, 2.0), DPoint(3.0, 4.0)) { dPoint, other -> dPoint.plus(other) } == DPoint(4.0, 6.0))
    require(consumeInline(DPoint(1.0, 2.0), DPoint(3.0, 4.0), DPoint::plus) == DPoint(4.0, 6.0))
    require(consumeInline(DPoint(1.0, 2.0), DPoint(3.0, 4.0)) { dPoint, other -> dPoint.plus(other) } == DPoint(4.0, 6.0))
    require(
        consume(DPoint(1.0, 2.0), DPoint(3.0, 4.0), fun(dPoint: DPoint, other: DPoint): DPoint = dPoint.plus(other)) ==
                DPoint(4.0, 6.0)
    )
    require(
        consumeInline(DPoint(1.0, 2.0), DPoint(3.0, 4.0), fun(dPoint: DPoint, other: DPoint): DPoint = dPoint.plus(other)) == 
                DPoint(4.0, 6.0)
    )

    require(makeDPoint(1.0, 2.0, ::DPoint) == DPoint(1.0, 2.0))
    require(makeDPointInline(1.0, 2.0, ::DPoint) == DPoint(1.0, 2.0))

    require(::DPoint == ::DPoint)
    require(::DPoint == any1())
    require(dPoint::f == dPoint::f)
    require(DPoint::f == DPoint::f)
    require(dPoint::f == any2(dPoint))
    require(DPoint::f == any2())
    require(dPoint::suspended == dPoint::suspended)
    require(DPoint::suspended == DPoint::suspended)
    require(dPoint::suspended == any3(dPoint))
    require(DPoint::suspended == any3())
    require(dPoint::suspendedInline == dPoint::suspendedInline)
    require(DPoint::suspendedInline == DPoint::suspendedInline)
    require(dPoint::suspendedInline == any4(dPoint))
    require(DPoint::suspendedInline == any4())
    require(dPoint::functionWithInlineClass == dPoint::functionWithInlineClass)
    require(DPoint::functionWithInlineClass == DPoint::functionWithInlineClass)
    require(dPoint::functionWithInlineClass == any5(dPoint))
    require(DPoint::functionWithInlineClass == any5())
    require(dPoint::extensionFunction == dPoint::extensionFunction)
    require(DPoint::extensionFunction == DPoint::extensionFunction)
    require(dPoint::extensionFunction == any6(dPoint))
    require(DPoint::extensionFunction == any6())
    require(dPoint::suspendFunctionWithLambda == dPoint::suspendFunctionWithLambda)
    require(DPoint::suspendFunctionWithLambda == DPoint::suspendFunctionWithLambda)
    require(dPoint::suspendFunctionWithLambda == any7(dPoint))
    require(DPoint::suspendFunctionWithLambda == any7())
    require(dPoint::suspendInlineFunctionWithLambda == dPoint::suspendInlineFunctionWithLambda)
    require(DPoint::suspendInlineFunctionWithLambda == DPoint::suspendInlineFunctionWithLambda)
    require(dPoint::suspendInlineFunctionWithLambda == any8(dPoint))
    require(DPoint::suspendInlineFunctionWithLambda == any8())
    require(::g == ::g)
    require(a::f == a::f)
    require(A::f == A::f)
    require(DPoint::plus == DPoint::plus)
    require(dPoint.let { DPoint(it.x * 2, it.y * 2) } == DPoint(2.0, 4.0))
    require(dPoint.let(::id) == DPoint(1.0, 2.0))
    
    runSuspend { require(requiresF(dPoint) { it } == dPoint) }
    runSuspend { require(requiresF(dPoint, F { it }) == dPoint) }
    
    return "OK"
}

// FILE: another.kt

fun any1(): Any = ::DPoint
fun any2(dPoint: DPoint): Any = dPoint::f
fun any2(): Any = DPoint::f
fun any3(dPoint: DPoint): Any = dPoint::suspended
fun any3(): Any = DPoint::suspended
fun any4(dPoint: DPoint): Any = dPoint::suspendedInline
fun any4(): Any = DPoint::suspendedInline
fun any5(dPoint: DPoint): Any = dPoint::functionWithInlineClass
fun any5(): Any = DPoint::functionWithInlineClass
fun any6(dPoint: DPoint): Any = dPoint::extensionFunction
fun any6(): Any = DPoint::extensionFunction
fun any7(dPoint: DPoint): Any = dPoint::suspendFunctionWithLambda
fun any7(): Any = DPoint::suspendFunctionWithLambda
fun any8(dPoint: DPoint): Any = dPoint::suspendInlineFunctionWithLambda
fun any8(): Any = DPoint::suspendInlineFunctionWithLambda

fun interface F {
    suspend fun run(dPoint: DPoint): DPoint
}

suspend fun requiresF(x: DPoint, f: F) = f.run(x)