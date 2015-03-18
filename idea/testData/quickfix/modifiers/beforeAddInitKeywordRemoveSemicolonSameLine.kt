// "Add 'init' keyword" "true"
fun foo() = 1
class A {
    val prop = foo()
    ;<caret>{

    }
}
