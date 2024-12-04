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
    <expr>c.property -= 20</expr>
    println(C().property)
}

// IGNORE_FE10
// FE1.0 `isUsedAsExpression` considers built-in postfix inc/dec and
// compound assignments as used, always.