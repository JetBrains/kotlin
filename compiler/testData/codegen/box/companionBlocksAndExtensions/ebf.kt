// ISSUE: KT-87192
// LANGUAGE: +CompanionBlocks +CompanionExtensions
// WITH_STDLIB
class C {
    companion {
        val foo: List<String>
            field: MutableList<String> = mutableListOf()

        fun insideCompanion() {
            foo.add("a")
            bar.add("a")
        }
    }

    companion object {
       fun insideCompanionObject() {
            foo.add("b")
            bar.add("b")
        }
    }

    fun insideClass() {
        foo.add("c")
        bar.add("c")
    }
}

companion val C.bar: List<String>
    field: MutableList<String> = mutableListOf()

companion fun C.companionExtension() {
    bar.add("d")
}

fun topLevel() {
    C.bar.add("e")
}

fun box(): String {
    C.insideCompanion()
    C.insideCompanionObject()
    C().insideClass()
    C.companionExtension()
    topLevel()

    if (C.foo.joinToString("") != "abc") return C.foo.joinToString()
    if (C.bar.joinToString("") != "abcde") return C.bar.joinToString()

    return "OK"
}
