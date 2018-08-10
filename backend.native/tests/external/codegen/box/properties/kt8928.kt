class App {
    fun init() {
        s = "OK"
    }
    @kotlin.native.ThreadLocal
    companion object {
        var s: String = "Fail"
            private set

    }
}

fun box(): String {
    App().init()

    return App.s
}