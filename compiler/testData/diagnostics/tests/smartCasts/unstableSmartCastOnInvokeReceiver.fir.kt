// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTICS_FULL_TEXT

interface SomeInterface {
    val interfaceMethod: (() -> Int)?
}

fun someFun(someInterface: SomeInterface) {
    someInterface.<!UNSAFE_IMPLICIT_INVOKE_CALL!>interfaceMethod<!>()

    if (someInterface.interfaceMethod != null) {
        someInterface.<!SMARTCAST_IMPOSSIBLE_ON_IMPLICIT_INVOKE_RECEIVER!>interfaceMethod<!>()
    }
}
