// "Make 'j' not abstract" "true"
class B {
    abstract<caret> var j: Int = 0
        set(v: Int) {}
}
/* FIR_COMPARISON */