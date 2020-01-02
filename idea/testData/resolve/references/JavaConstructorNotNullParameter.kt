// IGNORE_FIR

class A {
    fun foo() {
        <caret>JavaClass()
    }
}

// REF: (in JavaClass).JavaClass(String)
