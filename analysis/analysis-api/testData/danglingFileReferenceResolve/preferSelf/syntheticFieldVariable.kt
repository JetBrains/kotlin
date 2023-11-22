// COPY_RESOLUTION_MODE: PREFER_SELF

var x: Int = 0
    get() = f<caret>ield + 1
    set(value) { field = value - 1 }