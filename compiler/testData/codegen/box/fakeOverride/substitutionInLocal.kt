// MODULE: lib
// FILE: l.kt

open class LightParam : LightVariab<Unit>() { // i/o public String getName
}

open class LightVariab<W> {
    fun W.name(): String? = "O"
    val W.name2: String? get() = "K"
}

// MODULE: main(lib)
// FILE: m.kt

fun box(): String {
    return object : LightParam() {
        fun getText() = Unit.name() + Unit.name2
    }.getText()
}



