// TARGET_BACKEND: JVM
// LAMBDAS: CLASS
// WITH_STDLIB

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
