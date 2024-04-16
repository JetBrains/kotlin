// RENDER_DIAGNOSTICS_FULL_TEXT

interface SomeInterface {
    val interfaceMethod: (() -> Int)?
}

fun someFun(someInterface: SomeInterface) {
    someInterface.<!UNSAFE_IMPLICIT_INVOKE_CALL!>interfaceMethod<!>()

    if (someInterface.interfaceMethod != null) {
        someInterface.<!UNSAFE_IMPLICIT_INVOKE_CALL!>interfaceMethod<!>()
    }
}