// LANGUAGE: +ValueClasses
// TARGET_BACKEND: JVM_IR
// IGNORE_INLINER: IR
// CHECK_BYTECODE_LISTING
// WITH_STDLIB
// CHECK_BYTECODE_TEXT
// FIR_IDENTICAL

// FILE: caller.kt
import kotlin.coroutines.*

fun runSuspend(block: suspend () -> Unit) {
    block.startCoroutine(Continuation(EmptyCoroutineContext) { it.getOrThrow() })
}

// FILE: dependency.kt
import kotlin.math.sqrt

@JvmInline
value class DPoint(val x: Double, val y: Double)

fun Double.square() = this * this

@JvmInline
value class DSegment(val p1: DPoint, val p2: DPoint) {
    inline val length
        get() = sqrt((p1.x - p2.x).square() + (p1.y - p2.y).square())
    inline val middle
        get() = DPoint((p1.x + p2.x) / 2.0, (p1.y + p2.y) / 2.0)
}
inline val DSegment.length1
    get() = length
inline val DSegment.middle1
    get() = middle


inline fun DSegment.myLet(f: (DSegment) -> DPoint) = f(this)

// FILE: test.kt
import kotlin.coroutines.suspendCoroutine
import kotlin.coroutines.resume
import kotlin.math.sqrt

fun supply(x: Any?) = Unit

object InfiniteDPoints {
    class DPointsIterator {
        @PublishedApi
        internal var i = 1.0
        inline operator fun next() = DPoint(i, -i).also { i++ }
        inline operator fun hasNext() = true
    }
    operator fun iterator() = DPointsIterator()
}

fun forStatement() {
    var i = 1.0
    for (x in InfiniteDPoints) {
        require(x == DPoint(i, -i))
        i++
        if (i == 5.0) break
    }
}

fun box(): String {
    val point = DPoint(1.0, 2.0)
    val pointX2 = DPoint(2.0, 4.0)
    val segment = DSegment(point, pointX2)

    supply("a")
    point.let { it.x }
    supply("b")
    point.let { it }
    supply("c")
    run { DPoint (1.0, 2.0) }
    supply("d")
    val x = run { DPoint(100.0, 200.0) }
    supply("e")
    require(x == DPoint(100.0, 200.0))
    supply("f")
    point.let { DPoint(2 * it.x, 2 * it.y) }
    supply("g")
    require(point == point.let { it })
    supply("h")
    require(pointX2 == point.let { DPoint(2 * it.x, 2 * it.y) })
    supply("i")
    segment.myLet { it.p1 }
    supply("j")
    segment.myLet { it.p2 }
    supply("k")
    require(segment.myLet { it.p1 } == point)
    supply("l")
    require(segment.myLet { it.p2 } == pointX2)
    supply("m")
    require(segment.let { it.let { it } } == segment)
    supply("n")
    var a = 1
    segment.let { a++ }
    val b = segment.let { ++a }
    require(a == 3)
    supply("o")
    runSuspend { require(suspendFun() == DPoint(1.0, 2.0).toString()) }
    supply("p")
    require(segment.length == sqrt(5.0))
    supply("q")
    require(segment.length1 == sqrt(5.0))
    supply("r")
    require(segment.middle == DPoint(1.5, 3.0))
    supply("s")
    require(segment.middle1 == DPoint(1.5, 3.0))
    supply("t")
    require(segment.middle.x == 1.5)
    require(segment.middle.y == 3.0)
    supply("u")
    require(segment.middle1.x == 1.5)
    require(segment.middle1.y == 3.0)
    supply("v")
    forStatement()
    
    return "OK"
}

suspend fun f() = suspendCoroutine { it.resume(Unit) }
suspend fun suspendFun(): String {
    val x = run { f(); DPoint(1.0, 2.0) }
    return x.toString()
}

// @TestKt.class:
// 0 valueOf
// 0 INVOKE(STATIC|VIRTUAL) (DPoint|DSegment).*\.(un)?box
// 0 INVOKE(STATIC|VIRTUAL) .*(stub_for_inlining|lambda)
// 0 DCONST_0
