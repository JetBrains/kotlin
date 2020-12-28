val x = "Hello"

val y = "$<!REDUNDANT_SINGLE_EXPRESSION_STRING_TEMPLATE!>x<!>"

val z = "${y.hashCode()}"

fun toString(x: String) = "IC$x"

data class ProductGroup(val short_name: String, val parent: ProductGroup?) {
    val name: String = if (parent == null) short_name else "${parent.name} $short_name"
}