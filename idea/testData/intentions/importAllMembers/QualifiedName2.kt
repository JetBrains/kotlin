// INTENTION_TEXT: "Import members from 'javax.swing.SwingUtilities'"
// WITH_RUNTIME

fun foo() {
    <caret>javax.swing.SwingUtilities.invokeLater { }
}
