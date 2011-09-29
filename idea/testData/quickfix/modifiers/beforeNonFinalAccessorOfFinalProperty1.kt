// "Make 'i' open" "true"
open class A() {
    val i: Int = 1
    <caret>open get(): Int = $i
}