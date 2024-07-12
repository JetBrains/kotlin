// ISSUE: KT-53422
// CHECK_TYPE_WITH_EXACT

fun test() {
    val buildee = buildFromValue(
        innerBuild <!BUILDER_INFERENCE_MULTI_LAMBDA_RESTRICTION!>{ setInnerTypeVariable(TargetType()) }<!>,
        <!BUILDER_INFERENCE_MULTI_LAMBDA_RESTRICTION!>{ <!BUILDER_INFERENCE_STUB_RECEIVER!>it<!>.<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>placeholderExtensionInvokeOnInnerBuildee<!>() }<!>
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
