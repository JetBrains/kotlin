package Hello

fun main(vararg args: String) {
    System.out.println("Hello!")
}

companion fun Array.main() {
    System.out.println("Wrong!")
}
