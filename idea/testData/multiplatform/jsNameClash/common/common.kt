package sample

expect interface <!LINE_MARKER("descr='Is subclassed by AbstractInput JSInput'"), LINE_MARKER("descr='Has actuals in JS'")!>Input<!>

abstract class <!LINE_MARKER("descr='Is subclassed by JSInput'")!>AbstractInput<!> : Input {
    val head: Int = null!!
}