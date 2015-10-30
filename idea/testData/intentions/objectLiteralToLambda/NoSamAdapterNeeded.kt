// WITH_RUNTIME

import javax.swing.SwingUtilities

fun bar() {
    SwingUtilities.invokeLater(<caret>object: Runnable {
        override fun run() {
            throw UnsupportedOperationException()
        }
    })
}