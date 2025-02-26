// ISSUE: KT-53422
// IGNORE_NATIVE: compatibilityTestMode=BACKWARD_2_0
// ^^^ Compiler v2.0.0: error: the type of a receiver hasn't been inferred yet. Please specify type argument for generic parameter 'PTV' of 'buildFromValue' explicitly

// IGNORE_BACKEND_K1: ANY
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
