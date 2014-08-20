import java.io.Serializable

public data class Pair<out A, out B> (
        public val first: A,
        public val second: B
) : Serializable

fun box(): String {
    val p = Pair(42, "OK")
    val q = Pair(42, "OK")
    if (p != q) return "Fail equals"
    if (p.hashCode() != q.hashCode()) return "Fail hashCode"
    if (p.toString() != q.toString()) return "Fail toString"
    return p.second
}
