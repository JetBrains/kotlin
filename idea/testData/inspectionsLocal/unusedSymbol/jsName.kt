// PROBLEM: none
// JS
class Person(val name: String) {
    fun hello() {
        println("Hello $name!")
    }

    @JsName("helloWithGreeting")
    fun <caret>hello(greeting: String) {
        println("$greeting $name!")
    }
}