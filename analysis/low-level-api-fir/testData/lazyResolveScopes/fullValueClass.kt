// LANGUAGE: +FullValueClasses
package lowlevel

value class Po<caret>int(val x: Int, val y: Int) {
    fun sum(i: Int) = x + y + i
    val doubled get() = sum(x)
}
