// "Create parameter 'name'" "true"
fun f() {
    object : A(<caret>name) {

    }
}

open class A(s: String)