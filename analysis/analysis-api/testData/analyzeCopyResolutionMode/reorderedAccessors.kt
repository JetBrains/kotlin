// MODULE: original
var x: String
    get() {
        return ""
    }
    set(v: String) {
        // test
    }
// MODULE: copy
var x: String
    set(v: String) {
        // test
    }
    get() {
        return ""
    }