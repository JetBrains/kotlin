interface A {
    val str: String
        get() = "a"
}

class AB : A {
    override val str: String
        get() = super.str + "B"
}

val a = AB()
a.str<caret>