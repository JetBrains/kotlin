// MODULE: lib
// FILE: JsPrimitives.kt
package lib

interface JsObject {
    val isString: Boolean
        get() = false
}

fun interface JsRunnable : JsObject {
    fun run()
}

// MODULE: main(lib)
// FILE: main.kt
import lib.JsRunnable

class ReactComponent {
    fun forceUpdate(callback: JsRunnable) { callback.run() }
}

fun forceUpdate(myNativeComponent: ReactComponent, callback: () -> Unit) {
    myNativeComponent.forceUpdate(callback)
}

fun box(): String {
    return "OK"
}
