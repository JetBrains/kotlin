// "Make 'i' not abstract" "true"
class B {
    <caret>abstract val i: Int = 0
        get() = $i
}