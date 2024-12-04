class C {

    var property: Int = 58
        get() {
            return field * 2
        }
        set(value) {
            <expr>field</expr> += 45
        }

}

fun main() {
    val c = C()
    c.property -= 20
    println(C().property)
}