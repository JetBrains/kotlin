// "Remove useless cast" "true"
fun test() {
    class A()
    ({ "" } as<caret> () -> String)
}