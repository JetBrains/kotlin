interface Bar

operator fun Bar.get(n: Int): Int = 42

private operator fun Int.timesAssign(s: String) {}

fun usageBar(bar: Bar) {
    <expr>bar[1] *= "bar"</expr>
}

