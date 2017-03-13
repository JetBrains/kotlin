// FILE: Outer.kt

package another
open class Outer {
    protected class Stage(val run: () -> Unit)
    protected class My(var stage: Stage? = null) {
        fun initStage(f: () -> Unit): Stage {
            stage = Stage(f)
            return stage!!
        }
    }
    protected fun my(init: My.() -> Unit): My {
        val result = My()
        result.init()
        return result
    }
}

// FILE: Main.kt

package other
class Derived : another.Outer() {
    init {
        my {
            initStage { }
        }
    }
}

fun box(): String {
    Derived()
    return "OK"
}
