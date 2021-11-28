// FIR_IDENTICAL
class ShortenReferences {
    companion object {
        val DEFAULT = ShortenReferences()
    }

    fun process(
        element: String,
        elementFilter: (String) -> Int = { 10 },
        actionRunningMode: String = ""
    ): String {
        return "hello"
    }

    fun process(
        element: String,
        elementFilter: (String) -> Int = { 10 },
    ): String {
        return "hello"
    }
}



fun takeReference(block: (String) -> Unit) {}

fun test() {
    takeReference(ShortenReferences.DEFAULT::process)
}
