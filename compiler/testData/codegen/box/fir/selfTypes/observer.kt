// TARGET_BACKEND: JVM_IR

// WITH_STDLIB
// FULL_JDK

import java.awt.Color

@Self
abstract class AbstractObservable {
    private val observers: MutableList<(Self) -> Unit> = mutableListOf()

    fun observe(observer: (Self) -> Unit) {
        observers += observer
    }

    protected fun notification() {
        observers.forEach { observer ->
            observer(this as Self)
        }
    }
}

class Element : AbstractObservable<Element>() {
    var color: Color = Color.WHITE
        set(value) {
            field = value
            notification()
        }
}

fun box(): String {
    val colors: MutableList<Color> = mutableListOf()

    val element = Element().apply {
        observe {
            colors.add(it.color)
        }
    }

    element.color = Color.BLACK
    element.color = Color.DARK_GRAY

    return if (colors.size == 2 && colors.contains(java.awt.Color.BLACK) && colors.contains(java.awt.Color.DARK_GRAY))
        "OK"
    else
        "ERROR"
}
