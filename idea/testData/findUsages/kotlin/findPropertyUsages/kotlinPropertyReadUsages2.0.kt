// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtParameter
// OPTIONS: usages, skipWrite
// FIR_IGNORE

package server

open class A<T>(open var <caret>foo: T)

open class B: A<String>() {
    open var foo: String
        get() {
            println("get")
            return super<A>.foo
        }
        set(value: String) {
            println("set:" + value)
            super<A>.foo = value
        }
}

// DISABLE-ERRORS