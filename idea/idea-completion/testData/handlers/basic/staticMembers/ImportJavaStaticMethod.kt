import javax.swing.SwingUtilities.invokeLater

fun foo() {
    invoke<caret>
}

// INVOCATION_COUNT: 1
// ELEMENT_TEXT: "SwingUtilities.invokeAndWait"
// TAIL_TEXT: " {...} ((() -> Unit)!) (javax.swing)"
