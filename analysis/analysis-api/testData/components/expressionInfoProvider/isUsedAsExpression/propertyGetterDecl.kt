class C {

    var property: Int = 58
        get() {
            return <expr>field</expr> * 2
        }
        set(value) {
            field += 45
        }

}

fun main() {
    val c = C()
    c.property -= 20
    println(C().property)
}