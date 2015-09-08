import test.*

var p = "fail"

fun test() {
    foo {
        try {
            p = "O"
            return
        } catch(e: Exception) {
            return
        } finally {
            p += "K"
        }
    }
}

fun box(): String {
    test()
    return p
}