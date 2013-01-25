class Greeter(var name : String) {
    fun greet() {
        name = name.plus("")
        System.out?.println("Hello, $name");
    }
}

fun box() : String {
    Greeter("OK").greet()
    return "OK"
}
