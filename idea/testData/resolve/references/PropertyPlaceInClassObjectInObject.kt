package testing

object Testing {
    default object {
        <caret>va
    }
}

/// Should not fall on temp references in invalid code
// REF_EMPTY