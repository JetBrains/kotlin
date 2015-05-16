fun foo(): Boolean {
    ret<caret>
}

// ORDER: return
// ORDER: return false
// ORDER: return true
