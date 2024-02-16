fun test() {
    // simple function call
    build {
        consumeA(this)
    }

    // function call with a named argument
    build {
        consumeB(base = this)
    }

    // local function definition with a default parameter value
    build {
        fun consumeC(base: BaseBuildee<TargetType> = this) {}
    }

    // local function definition with a return type and a block body
    build {
        fun consumeD(): BaseBuildee<TargetType> { return this }
    }

    // local function definition with a return type and an expression body
    build {
        fun consumeE(): BaseBuildee<TargetType> = this
    }

    // call of a function with a vararg parameter
    build {
        consumeF(this, this)
    }

    // call of an extension function
    build {
        this.consumeG()
    }

    // call of an infix function
    build {
        this consumeH this
    }
}

open class BaseBuildee<BBTV>
class DerivedBuildee<DBTV>: BaseBuildee<DBTV>()

fun <FTV> build(instructions: DerivedBuildee<FTV>.() -> Unit): DerivedBuildee<FTV> {
    return DerivedBuildee<FTV>().apply(instructions)
}

class TargetType

fun consumeA(base: BaseBuildee<TargetType>) {}

fun consumeB(base: BaseBuildee<TargetType>) {}

fun consumeF(vararg bases: BaseBuildee<TargetType>) {}

fun BaseBuildee<TargetType>.consumeG() {}

infix fun BaseBuildee<TargetType>.consumeH(base: BaseBuildee<TargetType>) {}
