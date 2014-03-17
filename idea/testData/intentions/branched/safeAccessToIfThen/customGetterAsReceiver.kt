class Foo {
    val b: String?
        get() {
            print("I have side effects")
            return "Foo"
        }
}
fun main(args: Array<String>) {
    val a = Foo()
    println(a.b?.<caret>length)
}
