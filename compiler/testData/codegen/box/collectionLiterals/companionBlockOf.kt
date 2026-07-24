// IGNORE_BACKEND: ANDROID
// LANGUAGE: +CompanionBlocks +CompanionExtensions +CollectionLiterals
// WITH_STDLIB

class MyStringStorage(val s: String){
    companion {
        operator fun of(elem: String): MyStringStorage = MyStringStorage("")
        operator fun of(vararg elems: String): MyStringStorage = MyStringStorage(elems.joinToString(separator = ""))
    }
}

fun box(): String {
    val a: MyStringStorage = ["one-elem"]
    val b: MyStringStorage = ["O", ""]
    val c: MyStringStorage = []
    val d: MyStringStorage = ["", "K", ""]
    return a.s + b.s + c.s + d.s
}
