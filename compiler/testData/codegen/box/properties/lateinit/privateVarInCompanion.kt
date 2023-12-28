// IGNORE_BACKEND: JVM
// KT-44234

class App {
    val context: Context = Context()

    fun onCreate() {
        instance = this
    }

    companion object {
        private lateinit var instance: App set
        val context: Context get() = instance.context
    }

}

class Context {
    fun print(): String = "OK"
}

fun box(): String {
    val app = App()
    app.onCreate()
    return App.context.print()
}
