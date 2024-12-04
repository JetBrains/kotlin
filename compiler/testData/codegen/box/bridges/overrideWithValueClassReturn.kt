// WITH_STDLIB

interface I<E> {
    public fun foo(): VC<E>
}

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
public value class VC<out T>(val holder: String)

abstract class AC<E>(val vc: VC<E>) : I<E> {
    override fun foo(): VC<E> {
        return vc
    }
}

fun box(): String {
    return object : AC<String>(VC("OK")) {}.foo().holder
}
