fun test(n: Int): String {
    var res: String = "!"

    <caret>if (n == 1) res += "one" else res += "two"

    return res
}