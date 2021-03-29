// "Make 'companion object' not open" "true"
class A {
    <caret>open companion object {
        fun a(): Int = 1
    }
}
/* IGNORE_FIR */