val x
    get() = 1
    set(value) {
        <caret>
    }

// OUT_OF_BLOCK: true
// TODO should not be out of block
