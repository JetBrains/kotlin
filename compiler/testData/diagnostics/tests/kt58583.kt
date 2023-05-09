// FIR_IDENTICAL

// FILE: DialogWrapper.kt
package pkg

open class DialogWrapper {
    protected open class DialogWrapperAction
}

// FILE: Main.kt
import pkg.DialogWrapper

fun main() {
    object: DialogWrapper() {
        init {
            object: DialogWrapperAction() {}
        }
    }
}
