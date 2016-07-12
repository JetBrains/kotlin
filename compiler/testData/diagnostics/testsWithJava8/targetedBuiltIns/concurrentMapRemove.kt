import java.util.concurrent.*

val concurrent: ConcurrentMap<String, Int> = null!!
val concurrentHash: ConcurrentHashMap<String, Int> = null!!

fun foo() {
    // TODO: Too permissive
    concurrent.remove("", 1)
    concurrent.remove("", "")
    concurrentHash.remove("", 1)
    concurrentHash.remove("", "")

    // Flexible types
    concurrent.remove(null, 1)
    concurrent.remove(null, null)

    // @PurelyImplements
    concurrentHash.remove(null, 1)
    concurrentHash.remove(null, null)
}
