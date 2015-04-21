package inlineFun1

class A {
    inline fun component1() = foo { 1 }
    inline fun component2() = foo { 2 }

    inline fun foo(f: () -> Int) = f()
}