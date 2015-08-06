fun foo() {
    javax.swing.SwingUtilities.invoke<caret>
}

// EXIST_JAVA_ONLY: { lookupString: "invokeLater", itemText: "invokeLater", tailText: "(Runnable!)", typeText: "Unit" }
// EXIST_JAVA_ONLY: { lookupString: "invokeLater", itemText: "invokeLater", tailText: " {...} ((() -> Unit)!)", typeText: "Unit" }
