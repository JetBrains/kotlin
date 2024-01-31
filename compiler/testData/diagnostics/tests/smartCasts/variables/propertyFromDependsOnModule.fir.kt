// SKIP_TXT

// MODULE: transitive
// FILE: Transitive.kt
class Transitive {
    val finalProperty: Any = "";
}


// MODULE: direct()()(transitive)
// FILE: Direct.kt
class Direct {
    val finalProperty: Any = "";
}

// MODULE: app()()(direct)
fun isCast(d: Direct, t: Transitive) {
    if (d.finalProperty is String) {
        d.finalProperty.length
    }

    if (t.finalProperty is String) {
        t.finalProperty.length
    }
}

fun asCast(d: Direct, t: Transitive) {
    d.finalProperty as String
    d.finalProperty.length

    t.finalProperty as String
    t.finalProperty.length
}
