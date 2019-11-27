// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// WITH_RUNTIME

class A {
    fun f(): () -> String {
        val s = "OK"
        return { -> s }
    }
}

fun box(): String {
    val lambdaClass = A().f().javaClass
    val fields = lambdaClass.getDeclaredFields().toList()
    if (fields.size != 1) return "Fail: lambda should only capture 's': $fields"

    val fieldName = fields[0].getName()
    if (fieldName != "\$s") return "Fail: captured variable should be named '\$s': $fields"

    return "OK"
}
