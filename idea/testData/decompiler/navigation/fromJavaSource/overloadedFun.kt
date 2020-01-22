package testData.libraries

@JvmOverloads
fun <T> String.overloadedFun(vararg specs: String, allowExisting: Boolean = false, x: Int, y: Int = 2, z: T): String {
    TODO()
}