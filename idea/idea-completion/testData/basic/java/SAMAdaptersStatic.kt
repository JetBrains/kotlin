fun foo() {
    javax.swing.SwingUtilities.invoke<caret>
}

// EXIST: { lookupString: "invokeLater", itemText: "invokeLater", tailText: "(Runnable!)", typeText: "Unit" }
// EXIST: { lookupString: "invokeLater", itemText: "invokeLater", tailText: " {...} ((() -> Unit)!)", typeText: "Unit" }
