class App {
    fun init() {
        s = "OK"
    }
    @konan.ThreadLocal
    companion object {
        var s: String = "Fail"
            private set

    }
}

fun box(): String {
    App().init()

    return App.s
}