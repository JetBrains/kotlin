// callable: test/C.x
package test

interface A<T> {
    val x: T
}

interface B<T: CharSequence> : A<T>

interface C : B<String>