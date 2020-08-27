// ISSUE: KT-41430

class A

fun test_1(list: List<Set<A>>) {
    list.<!AMBIGUITY!>flatMapTo<!>(mutableSetOf()) { <!UNRESOLVED_REFERENCE!>it<!> }
}

fun test_2(list: List<Set<A>>) {
    sequence<A> {
        <!DEBUG_INFO_EXPRESSION_TYPE("ERROR CLASS: Ambiguity: flatMapTo, [kotlin/collections/flatMapTo, kotlin/collections/flatMapTo]")!>list.<!AMBIGUITY!>flatMapTo<!>(mutableSetOf()) { <!UNRESOLVED_REFERENCE!>it<!> }<!>
    }
}

fun test_3(list: List<Set<A>>) {
    sequence {
        list.<!AMBIGUITY!>flatMapTo<!>(mutableSetOf()) { <!UNRESOLVED_REFERENCE!>it<!> }
        yield(A())
    }
}

fun test_4(list: List<Set<A>>) {
    sequence {
        yield(A())
        list.<!AMBIGUITY!>flatMapTo<!>(mutableSetOf()) { <!UNRESOLVED_REFERENCE!>it<!> }
    }
}
