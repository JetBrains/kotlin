val String.countCharacters: Int
    get() = length

var Int.meaning: Long
    get() = 42L
    set(value) {}

fun test() {
    val f = String::countCharacters
    
    f : KExtensionProperty<String, Int>
    <!TYPE_MISMATCH!>f<!> : KMutableExtensionProperty<String, Int>
    f.get("abc") : Int
    f.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>set<!>("abc", 0)

    val g = Int::meaning

    g : KExtensionProperty<Int, Long>
    g : KMutableExtensionProperty<Int, Long>
    g.get(0) : Long
    g.set(1, 0L)
}
