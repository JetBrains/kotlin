fun foo() {
    invoke<caret>
}

// INVOCATION_COUNT: 2
// ELEMENT_TEXT: "SwingUtilities.invokeLater"
