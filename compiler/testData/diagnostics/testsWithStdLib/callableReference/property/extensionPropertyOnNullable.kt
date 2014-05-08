val Any?.meaning: Int
    get() = 42

fun test() {
    val f = Any?::meaning
    f.get(null) : Int
    f.get("") : Int
}
