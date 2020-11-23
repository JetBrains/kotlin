class Some {
    val x: String = "Some"
}

actual typealias <!LINE_MARKER("descr='Has declaration in common module'")!>TypeAlias<!> = Some

