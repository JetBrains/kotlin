typealias X<XT> = List<X>
class A {
    typealias Y<YS, YV> = Map<YS, YV>

    class B {
        typealias Q<QS, QV> = MutableMap<QS, QV>
    }
}

fun foo() {
    typealias L<LS, LV> = Map<LS, LV>
}