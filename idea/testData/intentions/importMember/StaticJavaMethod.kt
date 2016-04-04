// INTENTION_TEXT: "Add import for 'javax.swing.SwingUtilities.invokeLater'"
// WITH_RUNTIME
// ERROR: Unresolved reference: SomethingElse
// ERROR: Unresolved reference: somethingElse
// SKIP_ERRORS_AFTER

import javax.swing.SwingUtilities

fun foo() {
    SwingUtilities.<caret>invokeLater {}
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