// Should not fall on temp references in invalid code

object Testing {
    companion object {
        @<caret>va
    }
}