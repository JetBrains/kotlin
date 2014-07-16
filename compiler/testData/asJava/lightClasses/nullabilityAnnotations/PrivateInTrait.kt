// PrivateInTrait

trait PrivateInTrait {
    private var nn: String
        get() = ""
        set(value) {}
    private val n: String?
        get() = ""
    private fun bar(a: String, b: String?): String?
}