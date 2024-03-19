// EnumNameOverride
interface Foo {
    fun name(): String
}

interface Bar : Foo {
    override fun name() = name
    var name: String
}

enum class EnumNameOverride : Bar
