// FIR_COMPARISON
import javax.swing.SwingUtilities

class A : SwingUtilities {
    fun foo() {
        invoke<caret>
    }
}

// INVOCATION_COUNT: 2
// ABSENT: { itemText: "SwingUtilities.invokeLater" }
// EXIST_JAVA_ONLY: { itemText: "invokeLater" }
