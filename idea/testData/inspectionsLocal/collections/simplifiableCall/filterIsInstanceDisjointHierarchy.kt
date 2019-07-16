// WITH_RUNTIME
// PROBLEM: none

interface Foo
interface Bar

fun instanceOfMarkerInterface(x: List<Foo>): List<Foo> = x.<caret>filter { it is Bar }