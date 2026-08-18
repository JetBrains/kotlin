@file:OptIn(ExperimentalVersionOverloading::class)

class MemberNameCollision {
    fun render(value: Int, @IntroducedAt("1") suffix: String = "K"): String = value.toString() + suffix

    fun render(value: String): String = value + "!"
}

fun box(): String {
    val collision = MemberNameCollision()
    if (collision.render("1") != "1!") return "FAIL hand-written overload"
    if (collision.render(1) != "1K") return "FAIL generated wrapper"
    if (collision.render(1, "K") != "1K") return "FAIL versioned overload"
    return "OK"
}
