// SKIP_JAVAC
// This directive is needed to skip this test in LazyBodyIsNotTouchedTilContractsPhaseTestGenerated,
//  because it fails to parse module structure of multimodule test
// ISSUE: KT-49127

// MODULE: lib
class Final<T>

open class Base<T>

class Derived<T> : Base<T>()

class FinalWithOverride<T> {
    override fun equals(other: Any?): Boolean {
        // some custom implementation
        return this === other
    }
}

// MODULE: main(lib)
fun testFinal(x: Final<*>, y: Final<Int>) {
    if (x == y) {
        // No error, even while `Final` belongs to a different module and "equals" contract might be changed without re-compilation
        // But since we had such behavior in FE1.0, it might be too strict to prohibit it now, especially once there's a lot of cases
        // when different modules belong to a single project, so they're totally safe (see KT-50534)
        takeIntFinal(x)
    }
    if (x === y) {
        takeIntFinal(x) // OK
    }
}

fun testBase(x: Base<*>, y: Base<Int>) {
    if (x == y) {
        takeIntBase(<!ARGUMENT_TYPE_MISMATCH!>x<!>) // Error
    }
    if (x === y) {
        takeIntBase(x) // OK
    }
}

fun testDerived(x: Derived<*>, y: Derived<Int>) {
    if (x == y) {
        // No error, even while `Final` belongs to a different module and "equals" contract might be changed without re-compilation
        // But since we had such behavior in FE1.0, it might be too strict to prohibit it now, especially once there's a lot of cases
        // when different modules belong to a single project, so they're totally safe (see KT-50534)
        takeIntDerived(x)
    }
    if (x === y) {
        takeIntDerived(x) // OK
    }
}

fun testFinalWithOverride(x: FinalWithOverride<*>, y: FinalWithOverride<Int>) {
    if (x == y) {
        takeIntFinalWithOverride(<!ARGUMENT_TYPE_MISMATCH!>x<!>) // Error
    }
    if (x === y) {
        takeIntFinalWithOverride(x) // OK
    }
}

fun takeIntFinal(x: Final<Int>) {}
fun takeIntBase(x: Base<Int>) {}
fun takeIntDerived(x: Derived<Int>) {}
fun takeIntFinalWithOverride(x: FinalWithOverride<Int>) {}
