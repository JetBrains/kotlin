val <T> T.valProp: T where T : Number, T : Int
    get() = this

fun box(): String {
    0.valProp

    return "OK"
}