fun test() {
    // a primary constructor call
    build {
        KlassA(this)
    }

    // a secondary constructor call
    build {
        KlassB(this)
    }

    // delegation to a super-constructor from a primary constructor
    build {
        class KlassC constructor(): BaseKlassC(this@build)
    }

    // delegation to a super-constructor from a secondary constructor
    build {
        class KlassD: BaseKlassD {
            constructor() : super(this@build)
        }
    }

    // inheritance via delegation
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>build<!> {
        class KlassE: BaseBuildee<TargetType> by this@build
    }

}

interface BaseBuildee<BBTV>
class DerivedBuildee<DBTV>: BaseBuildee<DBTV>

fun <FTV> build(instructions: DerivedBuildee<FTV>.() -> Unit): DerivedBuildee<FTV> {
    return DerivedBuildee<FTV>().apply(instructions)
}

class TargetType

class KlassA constructor(base: BaseBuildee<TargetType>)

class KlassB {
    constructor(base: BaseBuildee<TargetType>)
}

open class BaseKlassC(base: BaseBuildee<TargetType>)

open class BaseKlassD(base: BaseBuildee<TargetType>)
