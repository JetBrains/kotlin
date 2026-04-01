// setter: callable: test/C.x
package test

interface A<T> {
    var x: T
}

interface B<T: CharSequence> : A<T>

interface C : B<String>
