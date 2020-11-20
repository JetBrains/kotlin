expect sealed class <!LINE_MARKER("descr='Is subclassed by Sealed1 [common] Sealed1 in Sealed [jvm] Sealed2 [common] Sealed2 in Sealed [jvm]'"), LINE_MARKER("descr='Has actuals in JVM'")!>Sealed<!> {

    object <!LINE_MARKER("descr='Has actuals in JVM'")!>Sealed1<!> : Sealed

    class <!LINE_MARKER("descr='Has actuals in JVM'")!>Sealed2<!> : Sealed {
        val <!LINE_MARKER("descr='Has actuals in JVM'")!>x<!>: Int
        fun <!LINE_MARKER("descr='Has actuals in JVM'")!>foo<!>(): String
    }
}
