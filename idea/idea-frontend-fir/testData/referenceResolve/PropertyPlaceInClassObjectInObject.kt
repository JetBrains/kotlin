// Should not fall on temp references in invalid code
// UNRESOLVED_REFERENCE

object Testing {
    companion object {
        @<caret>va
    }
}