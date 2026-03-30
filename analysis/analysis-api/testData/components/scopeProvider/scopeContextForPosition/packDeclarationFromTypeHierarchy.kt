open class BaseProps {
    open val id: Int = 0
    val label: String = ""
}

class DerivedProps : BaseProps() {
    override val id: Int = 1
    val enabled: Boolean = false
}

fun target(...DerivedProps.$props) {
    <expr>id</expr>
    label.length
    enabled.not()
}
