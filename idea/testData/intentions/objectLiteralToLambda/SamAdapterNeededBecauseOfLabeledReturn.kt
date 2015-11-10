// WITH_RUNTIME

import javax.swing.SwingUtilities

fun bar(p: Int) {
    SwingUtilities.invokeLater(<caret>object: Runnable {
        override fun run() {
            if (p < 0) return
            throw UnsupportedOperationException()
        }
    })
}