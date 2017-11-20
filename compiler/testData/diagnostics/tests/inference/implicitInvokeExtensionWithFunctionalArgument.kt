// !WITH_NEW_INFERENCE
// !CHECK_TYPE
// !DIAGNOSTICS: -UNUSED_PARAMETER, -UNUSED_VARIABLE

class AbstractSelector<S, I>
class SelectorFor<S>

inline operator fun <S, I> SelectorFor<S>.invoke(f: S.() -> I): AbstractSelector<S, I> = TODO()

class State(val p1: Double, val p2: () -> Int, val p3: String?)

fun test(s: SelectorFor<State>): Double {
    val a = s { p1 }
    a checkType { _<AbstractSelector<State, Double>>() }

    val b = s { p2 }
    b checkType { _<AbstractSelector<State, () -> Int>>()}

    val c = s { p3 }
    c checkType { _<AbstractSelector<State, String?>>() }

    val d = s { }
    d checkType { _<AbstractSelector<State, Unit>>() }

    val e = s { return p1 }
    e checkType { _<AbstractSelector<State, Nothing>>() }

    <!UNREACHABLE_CODE!>return<!> null!!
}