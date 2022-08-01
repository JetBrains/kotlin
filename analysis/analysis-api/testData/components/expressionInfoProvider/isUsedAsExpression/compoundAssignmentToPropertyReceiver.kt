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
    val c = C()
    <expr>c</expr>.property -= 20
    println(C().property)
}