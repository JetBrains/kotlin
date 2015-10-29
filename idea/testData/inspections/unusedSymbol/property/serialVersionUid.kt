// FULL_JDK

class Klass : java.io.Serializable {
    companion object {
        private @JvmStatic val serialVersionUID: Long = 239
    }
}

fun main(args: Array<String>) {
    Klass()
}
