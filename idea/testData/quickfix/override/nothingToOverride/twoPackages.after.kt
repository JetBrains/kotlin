// "Change function signature to 'fun f(a: A)'" "true"
// ERROR: 'f' overrides nothing
import a.B
import a.A

class A {}
class BB : B() {
    override fun f(a: A) {}
}
