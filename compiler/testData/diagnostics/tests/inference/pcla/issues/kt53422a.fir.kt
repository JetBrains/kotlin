// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-53422
// CHECK_TYPE_WITH_EXACT

fun test() {
    val buildee = buildFromValue(
        innerBuild { setInnerTypeVariable(TargetType()) },
        { it.placeholderExtensionInvokeOnInnerBuildee() }
    )
    // exact type equality check — turns unexpected compile-time behavior into red code
    // considered to be non-user-reproducible code for the purposes of these tests
    checkExactType<Buildee<InnerBuildee<TargetType>>>(buildee)
}




class TargetType

class InnerBuildee<ITV> {
    fun setInnerTypeVariable(value: ITV) { storage = value }
    private var storage: ITV = null!!
}

fun <EITV> InnerBuildee<EITV>.placeholderExtensionInvokeOnInnerBuildee() {}

class Buildee<TV>

fun <IPTV> innerBuild(instructions: InnerBuildee<IPTV>.() -> Unit): InnerBuildee<IPTV> {
    return InnerBuildee<IPTV>().apply(instructions)
}

fun <PTV> buildFromValue(value: PTV, instructions: Buildee<PTV>.(PTV) -> Unit): Buildee<PTV> {
    return Buildee<PTV>().apply { instructions(value) }
}
