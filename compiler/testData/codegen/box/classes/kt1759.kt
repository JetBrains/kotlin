// Enable when JS backend supports Java class library, or consider replacing System.out.println with kotlin.println
// TARGET_BACKEND: JVM
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
