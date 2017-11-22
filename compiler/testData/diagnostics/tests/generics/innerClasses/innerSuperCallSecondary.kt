// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER
open class Super<T> {
    inner open class Inner {
    }
}

class Sub : Super<String>() {
    inner class SubInner : Super<String>.Inner {
        constructor()<!NI;UNRESOLVED_REFERENCE_WRONG_RECEIVER!><!>
        constructor(x: Int) : <!NI;UNRESOLVED_REFERENCE_WRONG_RECEIVER!>super<!>() {}
    }
}
