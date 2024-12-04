// callable: test/C.x
package test

interface A<T> {
    fun x(obj: T)
}

interface B<T: Number> : A<T>
interface C : B<Int>