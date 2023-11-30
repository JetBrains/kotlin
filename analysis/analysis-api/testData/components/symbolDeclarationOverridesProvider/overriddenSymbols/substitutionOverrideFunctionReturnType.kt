// callable: test/B.x
package test

interface A<T> {
    fun x(): T
}

interface B : A<Int>