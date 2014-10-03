import javax.swing.SwingUtilities

fun main(args : Array<String>) {
    SwingUtilities.invokeLater(object : Runnable {
        override fun run() {
            throw UnsupportedOperationException()
        }
    })
}
