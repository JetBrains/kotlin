// callable: test/B.x
package test

interface A<T> {
    fun x(obj: T)
}

interface B : A<Int>