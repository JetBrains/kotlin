// IGNORE_BACKEND: JVM
fun main() {
  println("FAIL")
}

@JvmOverloads
fun Array<String>.main(x: Int = 4, y: String = "Test") {
    println("OK")
}

// 0 INVOKESTATIC DontGenerateOnJvmOverloadsKt\.main ()V