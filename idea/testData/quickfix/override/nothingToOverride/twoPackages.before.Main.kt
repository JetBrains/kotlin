// "Change function signature to 'fun f(a: A)'" "true"
// ERROR: 'f' overrides nothing
import a.B
class A {}
class BB : B() {
    <caret>override fun f() {}
}
