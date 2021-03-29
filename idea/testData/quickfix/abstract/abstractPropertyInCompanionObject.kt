// "Make 'Owner' 'abstract'" "false"
// ERROR: Abstract property 'x' in non-abstract class 'Companion'
// ACTION: Make 'x' not abstract
// ACTION: Make internal

class Owner {
    companion object {
        <caret>abstract val x: Int
    }
}
