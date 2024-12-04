class C {

    var property: Int = 58
        get() {
            return field * 2
        }
        set(value) {
            field += 45
        }

}

fun main() {
    val c = <expr>C</expr>()
    c.property -= 20
    println(C().property)
}