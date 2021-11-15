// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND: ANDROID, ANDROID_IR
// WITH_STDLIB
// FULL_JDK

import java.awt.GridLayout
import javax.swing.JPanel

class Some {
    val baz get() = foo().layout

    companion object {
        private fun foo() = object : ButtonPanel() {
            init {
                layout = GridLayout()
            }
        }
    }
}

abstract class ButtonPanel : JPanel()

fun box() = if (Some().baz is GridLayout) "OK" else "FAIL"
