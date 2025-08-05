// LANGUAGE: +MultiPlatformProjects
// IGNORE_BACKEND_K2: ANY
// IGNORE_IR_DESERIALIZATION_TEST
// IGNORE_HMPP: ANY
// ISSUE: KT-79610
// MODULE: common
// FILE: commonMain.kt
enum class P2pState {
    INITIALIZING
}

abstract class ServerlessRTCClient {
    var p2pState: P2pState?
        internal set(value) {}
        get() = null
}

// MODULE: inter()()(common)
// MODULE: platform()()(inter)
// FILE: main.kt
class IOSClientWebRTC : ServerlessRTCClient() {
    fun init() {
        p2pState = P2pState.INITIALIZING
    }
}

fun box() = "OK"