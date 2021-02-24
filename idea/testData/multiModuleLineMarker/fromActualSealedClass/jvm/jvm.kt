actual sealed class <!LINE_MARKER("descr='Is subclassed by Sealed1 in Sealed [jvm] Sealed2 in Sealed [jvm]'"), LINE_MARKER("descr='Has declaration in common module'")!>Sealed<!> {

    actual object <!LINE_MARKER("descr='Has declaration in common module'")!>Sealed1<!> : Sealed()

    actual class <!LINE_MARKER("descr='Has declaration in common module'")!>Sealed2<!> : Sealed() {
        actual val <!LINE_MARKER("descr='Has declaration in common module'")!>x<!> = 42
        actual fun <!LINE_MARKER("descr='Has declaration in common module'")!>foo<!>() = ""
    }
}


