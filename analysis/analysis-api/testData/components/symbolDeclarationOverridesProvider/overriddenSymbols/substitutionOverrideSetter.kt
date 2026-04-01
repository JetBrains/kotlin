// setter: callable: test/B.x
package test

interface A<T> {
    var x: T
}

interface B : A<String>
