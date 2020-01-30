import javax.swing.SwingUtilities

fun main(args : Array<String>) {
    SwingUtilities.invokeLater(
            object : Thread(<caret>) {

            }
    )
}
