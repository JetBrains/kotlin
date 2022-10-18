// IGNORE_BACKEND_FIR: JVM_IR
// https://youtrack.jetbrains.com/issue/KT-52236/Different-modality-in-psi-and-fir
// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses
// CHECK_BYTECODE_LISTING
// FIR_IDENTICAL

interface AbstractPoint<T> {
    val x: T
    val y: T
}

@JvmInline
value class DPoint(override val x: Double, override val y: Double): AbstractPoint<Double>

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
value class GenericFakeOverrideMFVC(val field1: Double, val field2: Double) : GenericMFVCHolderWithImpls<DPoint>
@JvmInline
value class ReifiedFakeOverrideMFVC(val field1: Double, val field2: Double) : ReifiedMFVCHolderWithImpls
@JvmInline
value class GenericFakeOverrideMFVCWithMFVCUpperBound(val field1: Double, val field2: Double) : GenericMFVCHolderWithMFVCUpperBoundWithImpls<DPoint>


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
value class DPointWithInterface(val x: Double, val y: Double) : SomePointInterface<DPoint>, SomePointInterfaceWithMFVCBound<DPoint> {
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
    val dPoint = DPoint(1.0, 2.0)
    
    val lam1: () -> DPoint = { throw DataClassException(1) }
    val lam2: () -> Unit = { throw DataClassException(2 to dPoint) }
    val lam3: () -> DPoint = { throw DataClassException(3) }
    val lam4: () -> Unit = { throw DataClassException(4 to dPoint) }
    val lam5: () -> DPoint = { throw DataClassException(5) }
    val lam6: () -> Unit = { throw DataClassException(6 to dPoint) }
    val lam7: () -> DPoint = { throw DataClassException(7) }
    val lam8: () -> Unit = { throw DataClassException(8 to dPoint) }
    val lam9: () -> DPoint = { throw DataClassException(9) }
    val lam10: () -> Unit = { throw DataClassException(10 to dPoint) }
    val lam11: () -> DPoint = { throw DataClassException(11) }
    val lam12: () -> Unit = { throw DataClassException(12 to dPoint) }
    val lam13: () -> DPoint = { throw DataClassException(13) }
    val lam14: () -> Unit = { throw DataClassException(14 to dPoint) }
    val lam15: () -> DPoint = { throw DataClassException(15) }
    val lam16: () -> Unit = { throw DataClassException(16 to dPoint) }
    val lam17: () -> DPoint = { throw DataClassException(17) }
    val lam18: () -> Unit = { throw DataClassException(18 to dPoint) }
    val lam19: () -> Int = { throw DataClassException(19) }
    val lam20: () -> Unit = { throw DataClassException(20 to 1) }
    val emptyLam = {}
    val dPointLam = { dPoint }
    val otherDPoint = DPoint(3.0, 4.0)
    val otherDPointLam = { otherDPoint }
    equal({ "DPoint(x=1.0, y=2.0)" }, { dPoint.toString() })
    equal({ "DPoint(x=1.0, y=2.0)" }, { (dPoint as Any).toString() })

    equal({ true }, { dPoint.equals(dPoint) })
    equal({ true }, { dPoint.equals(dPoint as Any) })
    equal({ true }, { (dPoint as Any).equals(dPoint) })
    equal({ true }, { (dPoint as Any).equals(dPoint as Any) })

    equal({ false }, { dPoint.equals(otherDPoint) })
    equal({ false }, { dPoint.equals(otherDPoint as Any) })
    equal({ false }, { (dPoint as Any).equals(otherDPoint) })
    equal({ false }, { (dPoint as Any).equals(otherDPoint as Any) })

    equal({ dPoint.hashCode() }, { (dPoint as Any).hashCode() })

    equal({ 1.0 }, { dPoint.x })
    equal({ 1.0 }, { (dPoint as AbstractPoint<Double>).x })
    equal({ 2.0 }, { dPoint.y })
    equal({ 2.0 }, { (dPoint as AbstractPoint<Double>).y })


    val realOverride = RealOverride(dPoint)

    equal(dPointLam, { realOverride.p })
    equal(dPointLam, { (realOverride as GenericMFVCHolder<DPoint>).p })
    equal(lam13, { realOverride.p1 })
    equal(lam13, { (realOverride as GenericMFVCHolder<DPoint>).p1 })
    equal(dPointLam, { (realOverride as ReifiedMFVCHolder).p })
    equal(lam13, { realOverride.p1 })
    equal(lam13, { (realOverride as ReifiedMFVCHolder).p1 })
    equal(dPointLam, { (realOverride as GenericMFVCHolderWithMFVCUpperBound<DPoint>).p })
    equal(lam13, { (realOverride as GenericMFVCHolderWithMFVCUpperBound<DPoint>).p1 })

    
    equal(emptyLam, { realOverride.p = dPoint })
    equal(emptyLam, { (realOverride as GenericMFVCHolder<DPoint>).p = dPoint })
    equal(lam14, { realOverride.p1 = dPoint })
    equal(lam14, { (realOverride as GenericMFVCHolder<DPoint>).p1 = dPoint })
    equal(emptyLam, { (realOverride as ReifiedMFVCHolder).p = dPoint })
    equal(lam14, { (realOverride as ReifiedMFVCHolder).p1 = dPoint })
    equal(emptyLam, { (realOverride as GenericMFVCHolderWithMFVCUpperBound<DPoint>).p = dPoint })
    equal(lam14, { (realOverride as GenericMFVCHolderWithMFVCUpperBound<DPoint>).p1 = dPoint })


    val genericFakeOverride = GenericFakeOverride()

    equal(lam1, { genericFakeOverride.p })
    equal(lam1, { (genericFakeOverride as GenericMFVCHolderWithImpls<DPoint>).p })
    equal(lam3, { genericFakeOverride.p1 })
    equal(lam3, { (genericFakeOverride as GenericMFVCHolderWithImpls<DPoint>).p1 })
    val reifiedFakeOverride = ReifiedFakeOverride()
    equal(lam9, { reifiedFakeOverride.p })
    equal(lam9, { (reifiedFakeOverride as ReifiedMFVCHolderWithImpls).p })
    equal(lam11, { reifiedFakeOverride.p1 })
    equal(lam11, { (reifiedFakeOverride as ReifiedMFVCHolderWithImpls).p1 })
    val genericFakeOverrideWithMFVCUpperBound = GenericFakeOverrideWithMFVCUpperBound()
    equal(lam5, { genericFakeOverrideWithMFVCUpperBound.p })
    equal(lam5, { (genericFakeOverrideWithMFVCUpperBound as GenericMFVCHolderWithMFVCUpperBoundWithImpls<DPoint>).p })
    equal(lam7, { genericFakeOverrideWithMFVCUpperBound.p1 })
    equal(lam7, { (genericFakeOverrideWithMFVCUpperBound as GenericMFVCHolderWithMFVCUpperBoundWithImpls<DPoint>).p1 })
    
    equal(lam2, { genericFakeOverride.p = dPoint })
    equal(lam2, { (genericFakeOverride as GenericMFVCHolderWithImpls<DPoint>).p = dPoint })
    equal(lam4, { genericFakeOverride.p1 = dPoint })
    equal(lam4, { (genericFakeOverride as GenericMFVCHolderWithImpls<DPoint>).p1 = dPoint })
    equal(lam10, { reifiedFakeOverride.p = dPoint })
    equal(lam10, { (reifiedFakeOverride as ReifiedMFVCHolderWithImpls).p = dPoint })
    equal(lam12, { reifiedFakeOverride.p1 = dPoint })
    equal(lam12, { (reifiedFakeOverride as ReifiedMFVCHolderWithImpls).p1 = dPoint })
    equal(lam6, { genericFakeOverrideWithMFVCUpperBound.p = dPoint })
    equal(lam6, { (genericFakeOverrideWithMFVCUpperBound as GenericMFVCHolderWithMFVCUpperBoundWithImpls<DPoint>).p = dPoint })
    equal(lam8, { genericFakeOverrideWithMFVCUpperBound.p1 = dPoint })
    equal(lam8, { (genericFakeOverrideWithMFVCUpperBound as GenericMFVCHolderWithMFVCUpperBoundWithImpls<DPoint>).p1 = dPoint })
    
    
    val genericFakeOverrideMFVC = GenericFakeOverrideMFVC(1.0, 2.0)

    equal(lam1, { genericFakeOverrideMFVC.p })
    equal(lam1, { (genericFakeOverrideMFVC as GenericMFVCHolderWithImpls<DPoint>).p })
    equal(lam3, { genericFakeOverrideMFVC.p1 })
    equal(lam3, { (genericFakeOverrideMFVC as GenericMFVCHolderWithImpls<DPoint>).p1 })

    val reifiedFakeOverrideMFVC = ReifiedFakeOverrideMFVC(1.0, 2.0)
    equal(lam9, { reifiedFakeOverrideMFVC.p })
    equal(lam9, { (reifiedFakeOverrideMFVC as ReifiedMFVCHolderWithImpls).p })
    equal(lam11, { reifiedFakeOverrideMFVC.p1 })
    equal(lam11, { (reifiedFakeOverrideMFVC as ReifiedMFVCHolderWithImpls).p1 })

    val genericFakeOverrideMFVCWithMFVCUpperBound = GenericFakeOverrideMFVCWithMFVCUpperBound(1.0, 2.0)
    equal(lam5, { genericFakeOverrideMFVCWithMFVCUpperBound.p })
    equal(lam5, { (genericFakeOverrideMFVCWithMFVCUpperBound as GenericMFVCHolderWithMFVCUpperBoundWithImpls<DPoint>).p })
    equal(lam7, { genericFakeOverrideMFVCWithMFVCUpperBound.p1 })
    equal(lam7, { (genericFakeOverrideMFVCWithMFVCUpperBound as GenericMFVCHolderWithMFVCUpperBoundWithImpls<DPoint>).p1 })

    equal(lam2, { genericFakeOverrideMFVC.p = dPoint })
    equal(lam2, { (genericFakeOverrideMFVC as GenericMFVCHolderWithImpls<DPoint>).p = dPoint })
    equal(lam4, { genericFakeOverrideMFVC.p1 = dPoint })
    equal(lam4, { (genericFakeOverrideMFVC as GenericMFVCHolderWithImpls<DPoint>).p1 = dPoint })

    equal(lam10, { reifiedFakeOverrideMFVC.p = dPoint })
    equal(lam10, { (reifiedFakeOverrideMFVC as ReifiedMFVCHolderWithImpls).p = dPoint })
    equal(lam12, { reifiedFakeOverrideMFVC.p1 = dPoint })
    equal(lam12, { (reifiedFakeOverrideMFVC as ReifiedMFVCHolderWithImpls).p1 = dPoint })

    equal(lam6, { genericFakeOverrideMFVCWithMFVCUpperBound.p = dPoint })
    equal(lam6, { (genericFakeOverrideMFVCWithMFVCUpperBound as GenericMFVCHolderWithMFVCUpperBoundWithImpls<DPoint>).p = dPoint })
    equal(lam8, { genericFakeOverrideMFVCWithMFVCUpperBound.p1 = dPoint })
    equal(lam8, { (genericFakeOverrideMFVCWithMFVCUpperBound as GenericMFVCHolderWithMFVCUpperBoundWithImpls<DPoint>).p1 = dPoint })


    val dPointWithInterface = DPointWithInterface(1.0, 2.0)

    equal(lam15, { dPointWithInterface.somethingGeneric })
    equal(lam15, { (dPointWithInterface as SomePointInterface<DPoint>).somethingGeneric })
    equal(lam19, { dPointWithInterface.somethingRegular })
    equal(lam19, { (dPointWithInterface as SomePointInterface<DPoint>).somethingRegular })
    equal(lam17, { dPointWithInterface.somethingMFVC })
    equal(lam17, { (dPointWithInterface as SomePointInterface<DPoint>).somethingMFVC })

    equal(lam15, { (dPointWithInterface as SomePointInterfaceWithMFVCBound<DPoint>).somethingGeneric })
    equal(lam19, { (dPointWithInterface as SomePointInterfaceWithMFVCBound<DPoint>).somethingRegular })
    equal(lam17, { (dPointWithInterface as SomePointInterfaceWithMFVCBound<DPoint>).somethingMFVC })

    equal(lam16, { dPointWithInterface.somethingGeneric = dPoint })
    equal(lam16, { (dPointWithInterface as SomePointInterface<DPoint>).somethingGeneric = dPoint })
    equal(lam20, { dPointWithInterface.somethingRegular = 1 })
    equal(lam20, { (dPointWithInterface as SomePointInterface<DPoint>).somethingRegular = 1 })
    equal(lam18, { dPointWithInterface.somethingMFVC = dPoint })
    equal(lam18, { (dPointWithInterface as SomePointInterface<DPoint>).somethingMFVC = dPoint })

    equal(lam16, { (dPointWithInterface as SomePointInterfaceWithMFVCBound<DPoint>).somethingGeneric = dPoint })
    equal(lam20, { (dPointWithInterface as SomePointInterfaceWithMFVCBound<DPoint>).somethingRegular = 1 })
    equal(lam18, { (dPointWithInterface as SomePointInterfaceWithMFVCBound<DPoint>).somethingMFVC = dPoint })


    val dSegment = DSegment(dPoint, otherDPoint)

    equal(dPointLam, { dSegment.p1 })
    equal(otherDPointLam, { dSegment.p2 })
    equal({ 1.0 }, { dPoint.x })
    equal({ 1.0 }, { dSegment.p1.x })
    equal({ 3.0 }, { otherDPoint.x })
    equal({ 3.0 }, { dSegment.p2.x })
    equal({ 2.0 }, { dPoint.y })
    equal({ 2.0 }, { dSegment.p1.y })
    equal({ 4.0 }, { otherDPoint.y })
    equal({ 4.0 }, { dSegment.p2.y })
    equal(dPointLam, { (dSegment as AbstractSegment<DPoint>).p1 })
    equal(otherDPointLam, { (dSegment as AbstractSegment<DPoint>).p2 })
    equal({ 1.0 }, { (dSegment as AbstractSegment<DPoint>).p1.x })
    equal({ 3.0 }, { (dSegment as AbstractSegment<DPoint>).p2.x })
    equal({ 2.0 }, { (dSegment as AbstractSegment<DPoint>).p1.y })
    equal({ 4.0 }, { (dSegment as AbstractSegment<DPoint>).p2.y })

    return "OK"
}
