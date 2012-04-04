import javax.swing.SwingUtilities

fun main(args : Array<String>) {
    SwingUtilities.invokeLater(object : Runnable {

    <caret>    public override fun run() {
            throw UnsupportedOperationException()
        }
    })
}
