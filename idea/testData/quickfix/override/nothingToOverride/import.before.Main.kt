// "Change function signature to 'override fun f(a: A)'" "true"
// ERROR: 'f' overrides nothing
// ERROR: 'f' overrides nothing
import a.B
class BB : B() {
    <caret>override fun f() {}
}
