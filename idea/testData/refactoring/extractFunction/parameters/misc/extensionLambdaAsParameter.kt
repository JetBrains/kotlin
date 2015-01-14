// PARAM_TYPES: kotlin.String.() -> T
// PARAM_DESCRIPTOR: value-parameter val f: kotlin.String.() -> T defined in test
fun <T> test(f: String.() -> T): T {
    <selection>while (true) {
        val answer = "Hey!".f()
        return answer
    }</selection>
}