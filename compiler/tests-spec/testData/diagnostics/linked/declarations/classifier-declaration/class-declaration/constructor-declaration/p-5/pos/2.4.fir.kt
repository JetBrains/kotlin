// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION -UNREACHABLE_CODE
// SKIP_TXT


// TESTCASE NUMBER: 1
fun <T> List<T>.case1() {
    class Case1(val t: T)
    class A(val t: T)
    class B(val x: List<T>)
    class C(val c: () -> T)
    class E(val n: Nothing, val t: T)
}

// TESTCASE NUMBER: 2
val <T> List<T>.case2: Int
    get() = {
        class A(val t: T)
        class B(val x: List<T>)
        class C(val c: () -> T)
        class E(val n: Nothing=TODO(), val t: T)

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
        class A(val t: T)
        class B(val x: List<T>)
        class C(val c: () -> T)
        class E(val n: Nothing = TODO(), t: T)

        fun test() {
            A(this.first())
            B(this)
            C { this.last() }
            E(t = this[2])
        }
    }
    set(i: Unit) {
        class A(val t: T)
        class B(val x: List<T>)
        class C(val c: () -> T)
        class E( t: T, val n: Nothing)

        fun test() {
            A(this.first())
            B(this)
            C { this.last() }
            E(t = this[2], TODO())
        }
    }
