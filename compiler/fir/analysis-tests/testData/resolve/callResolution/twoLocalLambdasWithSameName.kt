// ISSUE: KT-45316
interface R

fun takeInt(x: Int) {}

fun test(fn: R.() -> String) { // (1)
    val renderer = object : R {
        fun render(fn: R.() -> Int) { // (2)
            val result = fn()
            takeInt(result)
        }
    }
}
