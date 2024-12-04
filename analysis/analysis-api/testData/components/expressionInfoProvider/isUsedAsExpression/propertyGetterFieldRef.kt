class C {

    var property: Int = 58
    <expr>get() {
            return field * 2
        }</expr>
        set(value) {
            field += 45
        }

}

fun main() {
    val c = C()
    c.property -= 20
    println(C().property)
}