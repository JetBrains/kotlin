// FULL_JDK

import kotlin.platform.platformStatic

class Klass : java.io.Serializable {
    companion object {
        private platformStatic val serialVersionUID: Long = 239
    }
}

fun main(args: Array<String>) {
    Klass()
}
