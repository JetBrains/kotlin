// MODULE: original
interface A {
    companion object {}
}

// MODULE: copy
// COMPILATION_ERRORS

aaaaaaaaaa
bbbbbbbbbbbbbb
interface A {
    ccccccccccccc
    companion object {
        dddddddddddd
    }
}