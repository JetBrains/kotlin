// "Make 'i' open" "true"
open class A() {
    open val i: Int = 1
    <caret>open get(): Int = $i
}