package test

open class TypeRef<T> {
    val type = target()

    private fun target(): String {
        val thisClass = this.javaClass
        val superClass = thisClass.genericSuperclass

        return superClass.toString()
    }
}



inline fun <reified T> typeWithMessage(message: String = "Hello"): String {
    val type = object : TypeRef<T>() {}
    val target = type.type

    return message + " " + target
}