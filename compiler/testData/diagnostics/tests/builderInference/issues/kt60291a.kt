// ISSUE: KT-60291
// CHECK_TYPE_WITH_EXACT
// WITH_STDLIB

fun test() {
    val buildee = selectBuildee(
        build <!BUILDER_INFERENCE_MULTI_LAMBDA_RESTRICTION!>{ setTypeVariable(TargetType()) }<!>,
        build <!BUILDER_INFERENCE_MULTI_LAMBDA_RESTRICTION!>{}<!>
    )
    // exact type equality check — turns unexpected compile-time behavior into red code
    // considered to be non-user-reproducible code for the purposes of these tests
    checkExactType<Buildee<TargetType>>(buildee)
}




fun <T> selectBuildee(vararg values: Buildee<T>): Buildee<T> = values.first()

class TargetType

class Buildee<TV> {
    fun setTypeVariable(value: TV) { storage = value }
    private var storage: TV = null!!
}

fun <PTV> build(instructions: Buildee<PTV>.() -> Unit): Buildee<PTV> {
    return Buildee<PTV>().apply(instructions)
}
