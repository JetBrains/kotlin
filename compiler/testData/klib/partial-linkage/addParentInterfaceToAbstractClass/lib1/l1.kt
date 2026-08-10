class ValueDummy(val value: Int)

interface FaceDummy {
    fun faceFun() = ValueDummy(596)
}

interface FaceContractDummy {
    fun contract(): ValueDummy
    val property: ValueDummy
}

interface SecondFaceContractDummy {
    fun contract(): ValueDummy
}

fun interface FunInterface {
    fun contract(): ValueDummy
}

abstract class ClassGetMoreInterface : FaceDummy

abstract class FakeOverrideIntersection : FaceDummy

abstract class ClassGetFunInterface : FaceDummy
