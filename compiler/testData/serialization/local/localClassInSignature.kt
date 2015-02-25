// CLASS_NAME_SUFFIX: $main$Local

fun main() {
    open class Local {
        fun param(l: Local) {}

        val returnType: Local = this

        fun Local.receiver() = this

        fun <T : Local, U : T> generic(t: T): U = null!!
    }
}
