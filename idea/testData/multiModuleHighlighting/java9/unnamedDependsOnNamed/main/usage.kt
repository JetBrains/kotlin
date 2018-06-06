import dependency.*
import dependency.J
import dependency.K
import dependency.impl.*
import dependency.impl.JImpl
import dependency.impl.KImpl

fun usage(): String {
    val j: J = J.getInstance()
    val k: K = K.getInstance()

    val jImpl: JImpl = J.getInstance()
    val kImpl: KImpl = K.getInstance()

    return "$j$k$jImpl$kImpl"
}
