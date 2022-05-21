// IGNORE_BACKEND_FIR: JVM_IR
// https://youtrack.jetbrains.com/issue/KT-52236/Different-modality-in-psi-and-fir
// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses
// CHECK_BYTECODE_LISTING

interface AbstractPoint<T> {
    val x: T
    val y: T
}

@JvmInline
value class MyDouble(val value: Double)

val Double.my
    get() = MyDouble(this)

@JvmInline
value class DPoint(override val x: MyDouble, override val y: MyDouble): AbstractPoint<MyDouble>

interface GenericMFVCHolder<T> {
    var p: T
    var p1: T
}

interface GenericMFVCHolderWithMFVCUpperBound<T : DPoint> {
    var p: T
    var p1: T
}

interface ReifiedMFVCHolder {
    var p: DPoint
    var p1: DPoint
}

data class DataClassException(val value: Any?): Exception()

interface GenericMFVCHolderWithImpls<T> {
    var p: T
        get() = throw DataClassException(1)
        set(value) = throw DataClassException(2 to value)

    var p1: T
        get() = throw DataClassException(3)
        set(value) = throw DataClassException(4 to value)
}

interface GenericMFVCHolderWithMFVCUpperBoundWithImpls<T : DPoint> {
    var p: T
        get() = throw DataClassException(5)
        set(value) = throw DataClassException(6 to value)

    var p1: T
        get() = throw DataClassException(7)
        set(value) = throw DataClassException(8 to value)
}

interface ReifiedMFVCHolderWithImpls {
    var p: DPoint
        get() = throw DataClassException(9)
        set(value) = throw DataClassException(10 to value)

    var p1: DPoint
        get() = throw DataClassException(11)
        set(value) = throw DataClassException(12 to value)
}

class RealOverride(override var p: DPoint) : GenericMFVCHolder<DPoint>, ReifiedMFVCHolder, GenericMFVCHolderWithMFVCUpperBound<DPoint> {
    override var p1: DPoint
        get() = throw DataClassException(13)
        set(value) = throw DataClassException(14 to value)
}

class GenericFakeOverride : GenericMFVCHolderWithImpls<DPoint>
class ReifiedFakeOverride : ReifiedMFVCHolderWithImpls
class GenericFakeOverrideWithMFVCUpperBound : GenericMFVCHolderWithMFVCUpperBoundWithImpls<DPoint>


@JvmInline
value class GenericFakeOverrideMFVC(val field1: MyDouble, val field2: MyDouble) : GenericMFVCHolderWithImpls<DPoint>
@JvmInline
value class ReifiedFakeOverrideMFVC(val field1: MyDouble, val field2: MyDouble) : ReifiedMFVCHolderWithImpls
@JvmInline
value class GenericFakeOverrideMFVCWithMFVCUpperBound(val field1: MyDouble, val field2: MyDouble) : GenericMFVCHolderWithMFVCUpperBoundWithImpls<DPoint>


interface SomePointInterface<T> {
    var somethingRegular: Int

    var somethingGeneric: T

    var somethingMFVC: DPoint
}

interface SomePointInterfaceWithMFVCBound<T : DPoint> {
    var somethingRegular: Int

    var somethingGeneric: T

    var somethingMFVC: DPoint
}

@JvmInline
value class DPointWithInterface(val x: MyDouble, val y: MyDouble) : SomePointInterface<DPoint>, SomePointInterfaceWithMFVCBound<DPoint> {
    override var somethingGeneric: DPoint
        get() = throw DataClassException(15)
        set(value) = throw DataClassException(16 to value)

    override var somethingMFVC: DPoint
        get() = throw DataClassException(17)
        set(value) = throw DataClassException(18 to value)

    override var somethingRegular: Int
        get() = throw DataClassException(19)
        set(value) = throw DataClassException(20 to value)
}


interface AbstractSegment<T> {
    val p1: T
    val p2: T
}

@JvmInline
value class DSegment(override val p1: DPoint, override val p2: DPoint): AbstractSegment<DPoint>

fun <T> equal(expected: () -> T, actual: () -> T) {
    val expectedResult = runCatching { expected() }
    val actualResult = runCatching { actual() }
    require(expectedResult == actualResult) { "Expected: $expectedResult\nActual: $actualResult" }
}

fun box(): String {
    val dPoint = DPoint(1.0.my, 2.0.my)
    equal({ dPoint.toString() }, { (dPoint as Any).toString() })

    equal({ dPoint.equals(dPoint) }, { dPoint.equals(dPoint as Any) })
    equal({ dPoint.equals(dPoint) }, { (dPoint as Any).equals(dPoint) })
    equal({ dPoint.equals(dPoint) }, { dPoint.equals(dPoint as Any) })
    equal({ dPoint.equals(dPoint) }, { (dPoint as Any).equals(dPoint as Any) })

    val otherDPoint = DPoint(3.0.my, 4.0.my)
    equal({ dPoint.equals(otherDPoint) }, { dPoint.equals(otherDPoint as Any) })
    equal({ dPoint.equals(otherDPoint) }, { (dPoint as Any).equals(otherDPoint) })
    equal({ dPoint.equals(otherDPoint) }, { dPoint.equals(otherDPoint as Any) })
    equal({ dPoint.equals(otherDPoint) }, { (dPoint as Any).equals(otherDPoint as Any) })

    equal({ dPoint.hashCode() }, { (dPoint as Any).hashCode() })

    equal({ dPoint.x }, { (dPoint as AbstractPoint<MyDouble>).x })
    equal({ dPoint.y }, { (dPoint as AbstractPoint<MyDouble>).y })


    val realOverride = RealOverride(dPoint)

    equal({ realOverride.p }, { (realOverride as GenericMFVCHolder<DPoint>).p })
    equal({ realOverride.p1 }, { (realOverride as GenericMFVCHolder<DPoint>).p1 })
    equal({ realOverride.p }, { (realOverride as ReifiedMFVCHolder).p })
    equal({ realOverride.p1 }, { (realOverride as ReifiedMFVCHolder).p1 })
    equal({ realOverride.p }, { (realOverride as GenericMFVCHolderWithMFVCUpperBound<DPoint>).p })
    equal({ realOverride.p1 }, { (realOverride as GenericMFVCHolderWithMFVCUpperBound<DPoint>).p1 })

    equal({ realOverride.p = dPoint }, { (realOverride as GenericMFVCHolder<DPoint>).p = dPoint })
    equal({ realOverride.p1 = dPoint }, { (realOverride as GenericMFVCHolder<DPoint>).p1 = dPoint })
    equal({ realOverride.p = dPoint }, { (realOverride as ReifiedMFVCHolder).p = dPoint })
    equal({ realOverride.p1 = dPoint }, { (realOverride as ReifiedMFVCHolder).p1 = dPoint })
    equal({ realOverride.p = dPoint }, { (realOverride as GenericMFVCHolderWithMFVCUpperBound<DPoint>).p = dPoint })
    equal({ realOverride.p1 = dPoint }, { (realOverride as GenericMFVCHolderWithMFVCUpperBound<DPoint>).p1 = dPoint })


    val genericFakeOverride = GenericFakeOverride()

    equal({ genericFakeOverride.p }, { (genericFakeOverride as GenericMFVCHolderWithImpls<DPoint>).p })
    equal({ genericFakeOverride.p1 }, { (genericFakeOverride as GenericMFVCHolderWithImpls<DPoint>).p1 })
    val reifiedFakeOverride = ReifiedFakeOverride()
    equal({ reifiedFakeOverride.p }, { (reifiedFakeOverride as ReifiedMFVCHolderWithImpls).p })
    equal({ reifiedFakeOverride.p1 }, { (reifiedFakeOverride as ReifiedMFVCHolderWithImpls).p1 })
    val genericFakeOverrideWithMFVCUpperBound = GenericFakeOverrideWithMFVCUpperBound()
    equal(
        { genericFakeOverrideWithMFVCUpperBound.p },
        { (genericFakeOverrideWithMFVCUpperBound as GenericMFVCHolderWithMFVCUpperBoundWithImpls<DPoint>).p }
    )
    equal(
        { genericFakeOverrideWithMFVCUpperBound.p1 },
        { (genericFakeOverrideWithMFVCUpperBound as GenericMFVCHolderWithMFVCUpperBoundWithImpls<DPoint>).p1 }
    )

    equal({ genericFakeOverride.p = dPoint }, { (genericFakeOverride as GenericMFVCHolderWithImpls<DPoint>).p = dPoint })
    equal({ genericFakeOverride.p1 = dPoint }, { (genericFakeOverride as GenericMFVCHolderWithImpls<DPoint>).p1 = dPoint })
    equal({ reifiedFakeOverride.p = dPoint }, { (reifiedFakeOverride as ReifiedMFVCHolderWithImpls).p = dPoint })
    equal({ reifiedFakeOverride.p1 = dPoint }, { (reifiedFakeOverride as ReifiedMFVCHolderWithImpls).p1 = dPoint })
    equal(
        { genericFakeOverrideWithMFVCUpperBound.p = dPoint },
        { (genericFakeOverrideWithMFVCUpperBound as GenericMFVCHolderWithMFVCUpperBoundWithImpls<DPoint>).p = dPoint }
    )
    equal(
        { genericFakeOverrideWithMFVCUpperBound.p1 = dPoint },
        { (genericFakeOverrideWithMFVCUpperBound as GenericMFVCHolderWithMFVCUpperBoundWithImpls<DPoint>).p1 = dPoint }
    )


    val genericFakeOverrideMFVC = GenericFakeOverrideMFVC(1.0.my, 2.0.my)

    equal({ genericFakeOverrideMFVC.p }, { (genericFakeOverrideMFVC as GenericMFVCHolderWithImpls<DPoint>).p })
    equal({ genericFakeOverrideMFVC.p1 }, { (genericFakeOverrideMFVC as GenericMFVCHolderWithImpls<DPoint>).p1 })

    val reifiedFakeOverrideMFVC = ReifiedFakeOverrideMFVC(1.0.my, 2.0.my)
    equal({ reifiedFakeOverrideMFVC.p }, { (reifiedFakeOverrideMFVC as ReifiedMFVCHolderWithImpls).p })
    equal({ reifiedFakeOverrideMFVC.p1 }, { (reifiedFakeOverrideMFVC as ReifiedMFVCHolderWithImpls).p1 })

    val genericFakeOverrideMFVCWithMFVCUpperBound = GenericFakeOverrideMFVCWithMFVCUpperBound(1.0.my, 2.0.my)
    equal(
        { genericFakeOverrideMFVCWithMFVCUpperBound.p }, 
        { (genericFakeOverrideMFVCWithMFVCUpperBound as GenericMFVCHolderWithMFVCUpperBoundWithImpls<DPoint>).p },
    )
    equal(
        { genericFakeOverrideMFVCWithMFVCUpperBound.p1 },
        { (genericFakeOverrideMFVCWithMFVCUpperBound as GenericMFVCHolderWithMFVCUpperBoundWithImpls<DPoint>).p1 },
    )

    equal({ genericFakeOverrideMFVC.p = dPoint }, { (genericFakeOverrideMFVC as GenericMFVCHolderWithImpls<DPoint>).p = dPoint })
    equal({ genericFakeOverrideMFVC.p1 = dPoint }, { (genericFakeOverrideMFVC as GenericMFVCHolderWithImpls<DPoint>).p1 = dPoint })

    equal({ reifiedFakeOverrideMFVC.p = dPoint }, { (reifiedFakeOverrideMFVC as ReifiedMFVCHolderWithImpls).p = dPoint })
    equal({ reifiedFakeOverrideMFVC.p1 = dPoint }, { (reifiedFakeOverrideMFVC as ReifiedMFVCHolderWithImpls).p1 = dPoint })

    equal(
        { genericFakeOverrideMFVCWithMFVCUpperBound.p = dPoint }, 
        { (genericFakeOverrideMFVCWithMFVCUpperBound as GenericMFVCHolderWithMFVCUpperBoundWithImpls<DPoint>).p = dPoint },
    )
    equal(
        { genericFakeOverrideMFVCWithMFVCUpperBound.p1 = dPoint },
        { (genericFakeOverrideMFVCWithMFVCUpperBound as GenericMFVCHolderWithMFVCUpperBoundWithImpls<DPoint>).p1 = dPoint },
    )


    val dPointWithInterface = DPointWithInterface(1.0.my, 2.0.my)

    equal({ dPointWithInterface.somethingGeneric }, { (dPointWithInterface as SomePointInterface<DPoint>).somethingGeneric })
    equal({ dPointWithInterface.somethingRegular }, { (dPointWithInterface as SomePointInterface<DPoint>).somethingRegular })
    equal({ dPointWithInterface.somethingMFVC }, { (dPointWithInterface as SomePointInterface<DPoint>).somethingMFVC })

    equal({ dPointWithInterface.somethingGeneric }, { (dPointWithInterface as SomePointInterfaceWithMFVCBound<DPoint>).somethingGeneric })
    equal({ dPointWithInterface.somethingRegular }, { (dPointWithInterface as SomePointInterfaceWithMFVCBound<DPoint>).somethingRegular })
    equal({ dPointWithInterface.somethingMFVC }, { (dPointWithInterface as SomePointInterfaceWithMFVCBound<DPoint>).somethingMFVC })

    equal(
        { dPointWithInterface.somethingGeneric = dPoint },
        { (dPointWithInterface as SomePointInterface<DPoint>).somethingGeneric = dPoint }
    )
    equal({ dPointWithInterface.somethingRegular = 1 }, { (dPointWithInterface as SomePointInterface<DPoint>).somethingRegular = 1 })
    equal({ dPointWithInterface.somethingMFVC = dPoint }, { (dPointWithInterface as SomePointInterface<DPoint>).somethingMFVC = dPoint })

    equal(
        { dPointWithInterface.somethingGeneric = dPoint }, 
        { (dPointWithInterface as SomePointInterfaceWithMFVCBound<DPoint>).somethingGeneric = dPoint }
    )
    equal(
        { dPointWithInterface.somethingRegular = 2 }, 
        { (dPointWithInterface as SomePointInterfaceWithMFVCBound<DPoint>).somethingRegular = 2 }
    )
    equal(
        { dPointWithInterface.somethingMFVC = dPoint},
        { (dPointWithInterface as SomePointInterfaceWithMFVCBound<DPoint>).somethingMFVC = dPoint }
    )


    val dSegment = DSegment(dPoint, otherDPoint)
    (dSegment as AbstractSegment<DPoint>).p1.x
    
    equal({ dPoint }, { dSegment.p1 })
    equal({ otherDPoint }, { dSegment.p2 })
    equal({ dPoint.x }, { dSegment.p1.x })
    equal({ otherDPoint.x }, { dSegment.p2.x })
    equal({ dPoint.y }, { dSegment.p1.y })
    equal({ otherDPoint.y }, { dSegment.p2.y })
    equal({ dSegment.p1 }, { (dSegment as AbstractSegment<DPoint>).p1 })
    equal({ dSegment.p2 }, { (dSegment as AbstractSegment<DPoint>).p2 })
    equal({ dSegment.p1.x }, { (dSegment as AbstractSegment<DPoint>).p1.x })
    equal({ dSegment.p2.x }, { (dSegment as AbstractSegment<DPoint>).p2.x })
    equal({ dSegment.p1.y }, { (dSegment as AbstractSegment<DPoint>).p1.y })
    equal({ dSegment.p2.y }, { (dSegment as AbstractSegment<DPoint>).p2.y })

    return "OK"
}
