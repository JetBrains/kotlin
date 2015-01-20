import java.util.ArrayList

trait A
trait B : A
trait C

fun foo(a: A,  cA: Collection<A>, cB: Collection<B>, cC: Collection<C>, cAny: Collection<Any>,
        lA: List<A>, lb: List<B>, lC: List<C>, lAny: List<Any>,
        aA: ArrayList<A>, aB: ArrayList<B>, aC: ArrayList<C>, aAny: ArrayList<Any>) {
    if (a in <caret>
}

// EXIST: cA
// ABSENT: cB
// ABSENT: cC
// EXIST: cAny
// EXIST: lA
// ABSENT: lB
// ABSENT: lC
// EXIST: lAny
// EXIST: aA
// ABSENT: aB
// ABSENT: aC
// EXIST: aAny
