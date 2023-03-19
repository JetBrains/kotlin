// CHECK_BYTECODE_LISTING
// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// IGNORE_INLINER: IR
// LANGUAGE: +ValueClasses

@JvmInline
value class DPoint(/*inline */val x: Double/* = 1.0*/, /*inline */val y: Double/* = 2.0*/) {
    fun f1(a: Int, b: Int = -1, c: DPoint = DPoint(-2.0, -3.0)) = listOf(this, x, y, a, b, c)
    
    companion object {
        inline operator fun invoke(): DPoint = DPoint(0.0, 0.0)
    }
}

object RegularObject {
    fun pointToString(x: DPoint? = DPoint()) = "$x"
}

@JvmInline
value class DSegment(/*inline */val p1: DPoint/* = DPoint(3.0, 4.0)*/, /*inline */val p2: DPoint/* = DPoint(5.0, 6.0)*/, /*inline */val n: Int/* = 7*/) {
    fun f2(a: Int, b: Int = -1, c: DPoint = DPoint(-2.0, -3.0)) = listOf(this, p1, p2, n, a, b, c)
}

data class Wrapper(val segment: DSegment = DSegment(DPoint(8.0, 9.0), DPoint(10.0, 11.0), 7), val n: Int = 12) {
    fun f3(a: Int, b: Int = -1, c: DPoint = DPoint(-2.0, -3.0)) = listOf(this, segment, n, a, b, c)
}

fun complexFun(a1: Double, a2: DPoint, a3: Double = a1 * a2.x * a2.y, a4: DPoint = DPoint(a2.x * a1 * a3, a2.y * a1 * a3)) = "$a1, $a2, $a3, $a4"

inline fun complexInlineFun(a1: Double, a2: DPoint, a3: Double = a1 * a2.x * a2.y, a4: DPoint = DPoint(a2.x * a1 * a3, a2.y * a1 * a3)) = "$a1, $a2, $a3, $a4"

fun getLineIntersectionPoint(out: DPoint = DPoint()): DPoint? {
    return getIntersectXY(out)
}

fun getIntersectXY(out: DPoint = DPoint()): DPoint? {
    return out
}

fun box(): String {
//    comments bellow are because MFVC primary constructors default parameters require support of inline arguments in regular functions
//    require(DPoint() == DPoint(1.0, 2.0)) { "${DPoint()} ${DPoint(1.0, 2.0)}" }
//    require(DPoint(3.0) == DPoint(3.0, 2.0)) { "${DPoint()} ${DPoint(3.0, 2.0)}" }
//    require(DPoint(x = 3.0) == DPoint(3.0, 2.0)) { "${DPoint()} ${DPoint(3.0, 2.0)}" }
//    require(DPoint(y = 3.0) == DPoint(1.0, 3.0)) { "${DPoint()} ${DPoint(1.0, 3.0)}" }
//    val defaultDPoint = DPoint()
    val defaultDPoint = DPoint(1.0, 2.0)
    require(defaultDPoint.f1(4) == listOf(DPoint(1.0, 2.0), 1.0, 2.0, 4, -1, DPoint(-2.0, -3.0))) {
        defaultDPoint.f1(4).toString()
    }
    require(defaultDPoint.f1(4, 1, DPoint(2.0, 3.0)) == listOf(DPoint(1.0, 2.0), 1.0, 2.0, 4, 1, DPoint(2.0, 3.0))) {
        defaultDPoint.f1(4, 1, DPoint(2.0, 3.0)).toString()
    }
    require(DPoint(-1.0, -2.0).f1(4) == listOf(DPoint(-1.0, -2.0), -1.0, -2.0, 4, -1, DPoint(-2.0, -3.0))) {
        defaultDPoint.f1(4).toString()
    }
    require(DPoint(-1.0, -2.0).f1(4, 1, DPoint(2.0, 3.0)) == listOf(DPoint(-1.0, -2.0), -1.0, -2.0, 4, 1, DPoint(2.0, 3.0))) {
        defaultDPoint.f1(4, 1, DPoint(2.0, 3.0)).toString()
    }

//    require(DSegment() == DSegment(DPoint(3.0, 4.0), DPoint(5.0, 6.0), 7)) { DSegment().toString() }
//    val defaultDSegment = DSegment()
    val defaultDSegment = DSegment(DPoint(3.0, 4.0), DPoint(5.0, 6.0), 7)
    require(defaultDSegment.f2(100) == listOf(defaultDSegment, DPoint(3.0, 4.0), DPoint(5.0, 6.0), 7, 100, -1, DPoint(-2.0, -3.0))) {
        defaultDSegment.f2(100).toString()
    }
    require(defaultDSegment.f2(100, b = 1) == listOf(defaultDSegment, DPoint(3.0, 4.0), DPoint(5.0, 6.0), 7, 100, 1, DPoint(-2.0, -3.0))) {
        defaultDSegment.f2(100, b = 1).toString()
    }

    require(Wrapper() == Wrapper(DSegment(DPoint(8.0, 9.0), DPoint(10.0, 11.0), 7), 12)) { Wrapper().toString() }
    require(Wrapper().f3(100) == listOf(Wrapper(), DSegment(DPoint(8.0, 9.0), DPoint(10.0, 11.0), 7), 12, 100, -1, DPoint(-2.0, -3.0))) {
        Wrapper().f3(100).toString()
    }
    require(Wrapper().f3(100, b = 1) == listOf(Wrapper(), DSegment(DPoint(8.0, 9.0), DPoint(10.0, 11.0), 7), 12, 100, 1, DPoint(-2.0, -3.0))) {
        Wrapper().f3(100, b = 1).toString()
    }

    require(complexFun(2.0, DPoint(3.0, 5.0)) == "2.0, ${DPoint(3.0, 5.0)}, 30.0, ${DPoint(180.0, 300.0)}") {
        complexFun(2.0, DPoint(3.0, 5.0))
    }
    require(complexFun(2.0, DPoint(3.0, 5.0), 7.0) == "2.0, ${DPoint(3.0, 5.0)}, 7.0, ${DPoint(42.0, 70.0)}") {
        complexFun(2.0, DPoint(3.0, 5.0), 7.0)
    }
    require(complexFun(2.0, DPoint(3.0, 5.0), a4 = DPoint(11.0, 13.0)) == "2.0, ${DPoint(3.0, 5.0)}, 30.0, ${DPoint(11.0, 13.0)}") {
        complexFun(2.0, DPoint(3.0, 5.0), a4 = DPoint(11.0, 13.0))
    }
    require(complexFun(2.0, DPoint(3.0, 5.0), 7.0, DPoint(11.0, 13.0)) == "2.0, ${DPoint(3.0, 5.0)}, 7.0, ${DPoint(11.0, 13.0)}") {
        complexFun(2.0, DPoint(3.0, 5.0), 7.0, DPoint(11.0, 13.0))
    }

    require(complexInlineFun(2.0, DPoint(3.0, 5.0)) == "2.0, ${DPoint(3.0, 5.0)}, 30.0, ${DPoint(180.0, 300.0)}") {
        complexInlineFun(2.0, DPoint(3.0, 5.0))
    }
    require(complexInlineFun(2.0, DPoint(3.0, 5.0), 7.0) == "2.0, ${DPoint(3.0, 5.0)}, 7.0, ${DPoint(42.0, 70.0)}") {
        complexInlineFun(2.0, DPoint(3.0, 5.0), 7.0)
    }
    require(complexInlineFun(2.0, DPoint(3.0, 5.0), a4 = DPoint(11.0, 13.0)) == "2.0, ${DPoint(3.0, 5.0)}, 30.0, ${DPoint(11.0, 13.0)}") {
        complexInlineFun(2.0, DPoint(3.0, 5.0), a4 = DPoint(11.0, 13.0))
    }
    require(complexInlineFun(2.0, DPoint(3.0, 5.0), 7.0, DPoint(11.0, 13.0)) == "2.0, ${DPoint(3.0, 5.0)}, 7.0, ${DPoint(11.0, 13.0)}") {
        complexInlineFun(2.0, DPoint(3.0, 5.0), 7.0, DPoint(11.0, 13.0))
    }
    
    require(RegularObject.pointToString() == "DPoint(x=0.0, y=0.0)") { RegularObject.pointToString() }
    require(getLineIntersectionPoint().toString() == "DPoint(x=0.0, y=0.0)") { getLineIntersectionPoint().toString() }
    
    return "OK"
}
