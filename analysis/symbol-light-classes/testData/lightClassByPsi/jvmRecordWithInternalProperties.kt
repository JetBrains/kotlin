// JVM_TARGET: 17
// LIBRARY_PLATFORMS: JVM

@JvmRecord
data class Rec(internal val i: Int, internal val u: UInt) {
    internal val body: Int get() = 1
}

// DECLARATIONS_NO_LIGHT_ELEMENTS: Rec.class[component2;copy]
// LIGHT_ELEMENTS_NO_DECLARATION: Rec.class[component2-pVg5ArA$main;copy-Qn1smSk]
