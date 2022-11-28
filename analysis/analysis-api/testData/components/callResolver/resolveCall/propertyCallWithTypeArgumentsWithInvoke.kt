fun main(f: F<String>) {
    <expr>f</expr><String>()
}
class F<T> {
    operator fun <K> invoke() {}
}