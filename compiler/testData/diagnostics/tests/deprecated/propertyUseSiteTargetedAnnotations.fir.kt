// !DIAGNOSTICS: -UNUSED_EXPRESSION

class PropertyHolder {
    @Deprecated("")
    val a1 = 1

    @property:Deprecated("")
    var a2 = ""

    @get:Deprecated("")
    public val withGetter: String = ""

    @set:Deprecated("")
    public var withSetter: String = ""
}

fun fn() {
    val holder = PropertyHolder()

    holder.a1
    holder.a2
    holder.withGetter
    holder.withSetter = "A"
}

fun literals() {
    PropertyHolder::a1
    PropertyHolder::a2
    PropertyHolder::withGetter
    PropertyHolder::withSetter
}
