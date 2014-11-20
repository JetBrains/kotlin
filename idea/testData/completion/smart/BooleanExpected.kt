fun foo(): Boolean = <caret>

// EXIST: { itemText: "true", attributes: "bold" }
// EXIST: { itemText: "false", attributes: "bold" }
// ABSENT: Boolean
