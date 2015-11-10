// INTENTION_TEXT: "Import members from 'javax.swing.SwingUtilities'"
// WITH_RUNTIME
// ERROR: Unresolved reference: unresolved

import javax.swing.SwingUtilities

fun foo() {
    SwingUtilities.invokeLater { }

    val bottom = <caret>SwingUtilities.BOTTOM

    SwingUtilities.invokeAndWait {
        SwingUtilities.invokeLater { }
    }

    val horizontal = javax.swing.SwingUtilities.HORIZONTAL

    SwingUtilities.unresolved
}
