// IGNORE_BACKEND: JVM_IR
object A {
    private const val a = "$"
    private const val b = "1234$a"
    private const val c = 10000
}

//check that constant initializers inlined

// 0 GETSTATIC
// 1 PUTSTATIC A.INSTANCE
// 1 PUTSTATIC
