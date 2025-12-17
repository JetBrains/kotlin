// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-79496
sealed interface A
sealed interface B : A
sealed interface C : B

class E : A
class F : B
abstract class G : B
class H : G(), C

fun test_1(a: A): Int = <!WHEN_ON_SEALED_GEEN_ELSE!>when (a) {
    is E -> 0
    is F -> 1
    is G -> 2
}<!>

fun bar(b: B): Int = <!WHEN_ON_SEALED_GEEN_ELSE!>when (b) {
    is F -> 0
    is G -> 1
}<!>

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, interfaceDeclaration, isExpression, sealed,
smartcast, whenExpression, whenWithSubject */
