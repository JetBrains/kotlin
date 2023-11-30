// ISSUE: KT-53422

// IGNORE_LIGHT_ANALYSIS
// IGNORE_BACKEND: ANY
// REASON: red code (see corresponding diagnostic test)

fun box(): String {
    buildFromValue(
        innerBuild { setInnerTypeVariable(TargetType()) },
        { it.placeholderExtensionInvokeOnInnerBuildee() }
    )
    return "OK"
}




class TargetType

class InnerBuildee<ITV> {
    fun setInnerTypeVariable(value: ITV) { storage = value }
    private var storage: ITV = TargetType() as ITV
}

fun <EITV> InnerBuildee<EITV>.placeholderExtensionInvokeOnInnerBuildee() {}

class Buildee<TV>

fun <IPTV> innerBuild(instructions: InnerBuildee<IPTV>.() -> Unit): InnerBuildee<IPTV> {
    return InnerBuildee<IPTV>().apply(instructions)
}

fun <PTV> buildFromValue(value: PTV, instructions: Buildee<PTV>.(PTV) -> Unit): Buildee<PTV> {
    return Buildee<PTV>().apply { instructions(value) }
}
