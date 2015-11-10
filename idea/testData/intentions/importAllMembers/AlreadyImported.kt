// INTENTION_TEXT: "Import members from 'javax.swing.SwingUtilities'"
// WITH_RUNTIME

import javax.swing.SwingUtilities
import javax.swing.SwingUtilities.invokeLater

fun foo() {
    <caret>SwingUtilities.invokeLater { }

    val bottom = SwingUtilities.BOTTOM

    SwingUtilities.invokeAndWait {
        SwingUtilities.invokeLater { }
    }

    val horizontal = javax.swing.SwingUtilities.HORIZONTAL
}
