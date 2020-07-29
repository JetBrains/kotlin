// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT


// TESTCASE NUMBER: 1
fun <T> List<T>.case1() {
    class Case1(var t: T)
    class A(var t: T)
    class B(var x: List<T>)
    class C(var c: () -> T)
    class E(var n: Nothing, var t: T)
}

// TESTCASE NUMBER: 2
val <T> List<T>.case2: Int
    get() = {
        class A(var t: T)
        class B(var x: List<T>)
        class C(var c: () -> T)
        class E(var n: Nothing=TODO(), var t: T)

        fun test() {
            A(this.first())
            B(this)
            C { this.last() }
            E(t = this[2])
        }

        1
    }()

// TESTCASE NUMBER: 3
var <T> List<T>.case3: Unit
    get() {
        class A(var t: T)
        class B(var x: List<T>)
        class C(var c: () -> T)
        class E(var n: Nothing = TODO(), t: T)

        fun test() {
            A(this.first())
            B(this)
            C { this.last() }
            E(t = this[2])
        }
    }
    set(i: Unit) {
        class A(var t: T)
        class B(var x: List<T>)
        class C(var c: () -> T)
        class E( t: T, var n: Nothing =TODO())

        fun test() {
            A(this.first())
            B(this)
            C { this.last() }
            E(t = this[2])
        }
    }
