class InlineAll {

    inline fun inline(s: (Int, Double, Double, String, Long) -> String,
               a1: Int, a2: Double, a3: Double, a4: String, a5: Long): String {
        return s(a1, a2, a3, a4, a5)
    }
}