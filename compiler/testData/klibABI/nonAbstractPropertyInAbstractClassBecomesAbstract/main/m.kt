import abitestutils.abiTest
import lib1.A
import lib2.B
import lib2.B1
import lib2.B2
import lib2.B3
import lib2.B4

fun box() = abiTest {
    val a: A = B()
    val b = B()
    expectFailure(nonImplementedCallable("property accessor 'foo1.<get-foo1>'", "class 'B'")) { a.foo1 }
    expectFailure(nonImplementedCallable("property accessor 'foo2.<get-foo2>'", "class 'B'")) { a.foo2 }
    expectFailure(nonImplementedCallable("property accessor 'bar1.<get-bar1>'", "class 'B'")) { a.bar1 }
    expectFailure(nonImplementedCallable("property accessor 'bar2.<get-bar2>'", "class 'B'")) { a.bar2 }
    expectSuccess(-42) { a.baz1 }
    expectSuccess(-42) { a.baz2 }
    expectFailure(nonImplementedCallable("property accessor 'foo1.<get-foo1>'", "class 'B'")) { b.unlinkedPropertyUsage }
    expectFailure(nonImplementedCallable("property accessor 'foo1.<get-foo1>'", "class 'B1'")) { B1() }
    expectFailure(nonImplementedCallable("property accessor 'foo2.<get-foo2>'", "class 'B2'")) { B2() }
    expectFailure(nonImplementedCallable("property accessor 'bar1.<get-bar1>'", "class 'B3'")) { B3() }
    expectFailure(nonImplementedCallable("property accessor 'bar2.<get-bar2>'", "class 'B4'")) { B4() }
}
