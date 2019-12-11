data class My(val x: Unit)

fun foo(my: My?): Int? {
    val x = my?.x
    // ?. is required here
    return x?.hashCode()
}