// callable: test/B.x
package test

interface A<T> {
    val x: T
}

interface B : A<String>