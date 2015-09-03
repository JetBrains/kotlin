// !DIAGNOSTICS: -UNUSED_EXPRESSION

class PropertyHolder {
    deprecated("")
    val a1 = 1

    @property:deprecated("")
    var a2 = ""

    @get:deprecated("")
    public val withGetter: String = ""

    @set:deprecated("")
    public var withSetter: String = ""
}

fun fn() {
    val holder = PropertyHolder()

    holder.<!DEPRECATED_SYMBOL_WITH_MESSAGE!>a1<!>
    holder.<!DEPRECATED_SYMBOL_WITH_MESSAGE!>a2<!>
    holder.<!DEPRECATED_SYMBOL_WITH_MESSAGE!>withGetter<!>
    holder.<!DEPRECATED_SYMBOL_WITH_MESSAGE!>withSetter<!> = "A"
}

fun literals() {
    PropertyHolder::<!DEPRECATED_SYMBOL_WITH_MESSAGE!>a1<!>
    PropertyHolder::<!DEPRECATED_SYMBOL_WITH_MESSAGE!>a2<!>
    PropertyHolder::withGetter
    PropertyHolder::withSetter
}