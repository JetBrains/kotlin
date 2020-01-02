annotation class base

@Target(AnnotationTarget.TYPE)
annotation class typed

@base class My(val x: @base @typed Int, y: @base @typed Int) {
    val z: @base @typed Int = y
    fun foo(): @base @typed Int = z
}
