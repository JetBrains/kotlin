fun foo() {
    i<caret>
}

// INVOCATION_COUNT: 2
// EXIST_JAVA_ONLY: { allLookupStrings: "invokeLater", itemText: "SwingUtilities.invokeLater", tailText: "(Runnable!) (javax.swing)", typeText: "Unit", attributes: "" }
// EXIST_JAVA_ONLY: { allLookupStrings: "invokeAndWait", itemText: "SwingUtilities.invokeAndWait", tailText: "(Runnable!) (javax.swing)", typeText: "Unit", attributes: "" }
// EXIST_JAVA_ONLY: { allLookupStrings: "invokeLater", itemText: "SwingUtilities.invokeLater", tailText: " {...} ((() -> Unit)!) (javax.swing)", typeText: "Unit", attributes: "" }
// ABSENT: { itemText: "SwingUtilities.installSwingDropTargetAsNecessary" }
