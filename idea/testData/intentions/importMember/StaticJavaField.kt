// INTENTION_TEXT: "Add import for 'javax.swing.SwingConstants.CENTER'"
// WITH_RUNTIME

import javax.swing.SwingConstants

fun foo() {
    val v = SwingConstants.CENTER

    SwingConstants.<caret>CENTER
}
