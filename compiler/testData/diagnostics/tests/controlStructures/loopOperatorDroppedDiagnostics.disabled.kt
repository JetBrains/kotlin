// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-91430
// LANGUAGE_FEATURE_TOGGLED: ReportMissingErrorsOnForLoopOperatorCalls
// WITH_STDLIB
class Trio
context(s: String) operator fun Trio.iterator(): Iterator<String> = listOf(s).iterator()
fun f0() { for (x in <!FOR_LOOP_OPERATOR_WILL_BECOME_ERROR, OTHER_ERROR_WITH_REASON!>Trio()<!>) {} }

class It1(var n: Int) { operator fun hasNext() = n-- > 0 }
class Seq1
operator fun Seq1.iterator() = It1(1)
context(s: String) operator fun It1.next(): String = s
fun f1() { for (x in <!FOR_LOOP_OPERATOR_WILL_BECOME_ERROR, OTHER_ERROR_WITH_REASON!>Seq1()<!>) {} }

class It2 { operator fun next() = "x" }
class Seq2
operator fun Seq2.iterator() = It2()
context(s: String) operator fun It2.hasNext(): Boolean = false
fun f2() { for (x in <!FOR_LOOP_OPERATOR_WILL_BECOME_ERROR, OTHER_ERROR_WITH_REASON!>Seq2()<!>) {} }

class Box3 { context(s: String) operator fun iterator(): Iterator<String> = listOf(s).iterator() }
fun f3() { for (x in <!FOR_LOOP_OPERATOR_WILL_BECOME_ERROR, OTHER_ERROR_WITH_REASON!>Box3()<!>) {} }

class P1 { private operator fun iterator(): Iterator<String> = listOf("p").iterator() }
fun s1() { for (x in <!FOR_LOOP_OPERATOR_WILL_BECOME_ERROR, OTHER_ERROR_WITH_REASON!>P1()<!>) println(x) }

class P5 { protected operator fun iterator(): Iterator<String> = listOf("p").iterator() }
fun s5() { for (x in <!FOR_LOOP_OPERATOR_WILL_BECOME_ERROR, OTHER_ERROR_WITH_REASON!>P5()<!>) println(x) }

class It6(var n: Int) { operator fun hasNext() = n-- > 0; private operator fun next() = "x" }
class Seq6
operator fun Seq6.iterator() = It6(1)
fun s6() { for (x in <!FOR_LOOP_OPERATOR_WILL_BECOME_ERROR, OTHER_ERROR_WITH_REASON!>Seq6()<!>) println(x) }

class It7 { private operator fun hasNext() = false; operator fun next() = "x" }
class Seq7
operator fun Seq7.iterator() = It7()
fun s7() { for (x in <!FOR_LOOP_OPERATOR_WILL_BECOME_ERROR, OTHER_ERROR_WITH_REASON!>Seq7()<!>) println(x) }

/* GENERATED_FIR_TAGS: assignment, classDeclaration, comparisonExpression, forLoop, funWithExtensionReceiver,
functionDeclaration, functionDeclarationWithContext, incrementDecrementExpression, integerLiteral, localProperty,
operator, primaryConstructor, propertyDeclaration, stringLiteral */
