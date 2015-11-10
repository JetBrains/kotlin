// INTENTION_TEXT: "Add import for 'javax.swing.SwingUtilities.invokeLater'"
// WITH_RUNTIME
// ERROR: No value passed for parameter p0
// ERROR: Unresolved reference: SomethingElse
// ERROR: Unresolved reference: somethingElse

import javax.swing.SwingUtilities

fun foo() {
    SwingUtilities.<caret>invokeLater()
}

fun bar() {
    javax.swing.SwingUtilities.invokeLater {
    }

    javax.swing.SwingUtilities.invokeLater(Runnable {
        SwingUtilities.invokeLater { }
    })

    SwingUtilities.invokeAndWait { }

    SomethingElse.invokeLater()

    somethingElse.SwingUtilities.invokeLater()
}