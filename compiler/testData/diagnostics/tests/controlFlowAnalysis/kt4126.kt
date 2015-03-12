public data class ProductGroup(val short_name: String, val parent: ProductGroup?) {
    val name: String = if (parent == null) short_name else "${<!DEBUG_INFO_SMARTCAST!>parent<!>.name} $short_name"
}
