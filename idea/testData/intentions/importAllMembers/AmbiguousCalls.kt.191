// INTENTION_TEXT: "Import members from 'javax.swing.SwingUtilities'"
// WITH_RUNTIME
// ERROR: None of the following functions can be called with the arguments supplied: <br>public open fun convertPoint(source: Component!, aPoint: Point!, destination: Component!): Point! defined in javax.swing.SwingUtilities<br>public open fun convertPoint(source: Component!, x: Int, y: Int, destination: Component!): Point! defined in javax.swing.SwingUtilities
// ERROR: None of the following functions can be called with the arguments supplied: <br>public open fun convertPoint(source: Component!, aPoint: Point!, destination: Component!): Point! defined in javax.swing.SwingUtilities<br>public open fun convertPoint(source: Component!, x: Int, y: Int, destination: Component!): Point! defined in javax.swing.SwingUtilities
// ERROR: Unresolved reference: unresolved

import javax.swing.SwingUtilities

fun foo() {
    <caret>SwingUtilities.convertPoint()

    val bottom = SwingUtilities.BOTTOM

    SwingUtilities.convertPoint()

    SwingUtilities.unresolved
}
