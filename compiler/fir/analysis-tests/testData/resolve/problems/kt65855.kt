// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-65855

// KT-65855: Modality of intersection override of interop properties
// In K/N CInterop, ObjC protocol properties are marked as FINAL.
// When a class inherits from two protocols having the same FINAL property,
// K2 visibility inference would set UNKNOWN modality for the intersection override
// instead of FINAL, which breaks klib serialization.
// This test simulates the pattern with regular Kotlin interfaces.

interface Protocol1 {
    val bounds: String
}

interface Protocol2 {
    val bounds: String
}

// Simulates UIView which inherits `bounds` from both UIDynamicItemProtocol and UICoordinateSpaceProtocol
class MyView : Protocol1, Protocol2 {
    override val bounds: String get() = "view"
}

fun test(view: MyView): String {
    return view.bounds
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, getter, interfaceDeclaration, override,
propertyDeclaration, stringLiteral */
