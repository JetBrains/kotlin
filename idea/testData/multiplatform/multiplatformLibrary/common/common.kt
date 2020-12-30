package sample

expect class <!LINE_MARKER("descr='Has actuals in JVM, JS'")!>Sample<!>() {
    fun <!LINE_MARKER("descr='Has actuals in JVM, JS'")!>checkMe<!>(): Int
}

expect object <!LINE_MARKER("descr='Has actuals in JVM, JS'")!>Platform<!> {
    val <!LINE_MARKER("descr='Has actuals in JVM, JS'")!>name<!>: String
}

fun hello(): String = "Hello from ${Platform.name}"