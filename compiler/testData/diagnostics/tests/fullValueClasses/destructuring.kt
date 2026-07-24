// LANGUAGE: +FullValueClasses +NameBasedDestructuring
// LANGUAGE_FEATURE_TOGGLED: EnableNameBasedDestructuringShortForm
// LANGUAGE_FEATURE_TOGGLED_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// FILE: J.java
public class J {
    public static <T> T makeFlexible(T t) {
        return t;
    }
}
// FILE: test.kt
value class A(val x: Int)

interface I {
    operator fun component1(): String = ""
}

class Box<T>(val item: T)

fun <<!CONFLICTING_UPPER_BOUNDS!>T<!>> shortFormParentheses(f: A, t: T, box: Box<out T>) where T : <!FINAL_UPPER_BOUND!>A<!>, T : I {
    if (true) {
        val (x: Int) = f
        for ((x: Int) in listOf(f)) {}
        f.let { (x: Int) -> }
    }
    if (true) {
        val (x: Int) = J.makeFlexible(f)
        for ((x: Int) in listOf(J.makeFlexible(f))) {}
        J.makeFlexible(f).let { (x: Int) -> }
    }
    if (<!IMPOSSIBLE_IS_CHECK_ERROR!>f is I<!>) {
        val (x: Int) = f
        for ((x: Int) in <!INFERRED_TYPE_VARIABLE_INTO_POSSIBLE_EMPTY_INTERSECTION!>listOf<!>(f)) {}
        f.<!INFERRED_TYPE_VARIABLE_INTO_POSSIBLE_EMPTY_INTERSECTION!>let<!> { (x: Int) -> }
    }
    if (true) {
        val (x: Int) = t
        for ((x: Int) in listOf(t)) {}
        t.let { (x: Int) -> }
    }
    if (true) {
        val (x: Int) = box.item
        for ((x: Int) in listOf(box.item)) {}
        box.item.let { (x: Int) -> }
    }
}

fun <<!CONFLICTING_UPPER_BOUNDS!>T<!>> fullFormParentheses(f: A, t: T, box: Box<out T>) where T : <!FINAL_UPPER_BOUND!>A<!>, T : I {
    if (true) {
        (val x: Int) = f
        for ((val x: Int) in listOf(f)) {}
        f.let { (val x: Int) -> }
    }
    if (true) {
        (val x: Int) = J.makeFlexible(f)
        for ((val x: Int) in listOf(J.makeFlexible(f))) {}
        J.makeFlexible(f).let { (val x: Int) -> }
    }
    if (<!IMPOSSIBLE_IS_CHECK_ERROR!>f is I<!>) {
        (val x: Int) = f
        for ((val x: Int) in <!INFERRED_TYPE_VARIABLE_INTO_POSSIBLE_EMPTY_INTERSECTION!>listOf<!>(f)) {}
        f.<!INFERRED_TYPE_VARIABLE_INTO_POSSIBLE_EMPTY_INTERSECTION!>let<!> { (val x: Int) -> }
    }
    if (true) {
        (val x: Int) = t
        for ((val x: Int) in listOf(t)) {}
        t.let { (val x: Int) -> }
    }
    if (true) {
        (val x: Int) = box.item
        for ((val x: Int) in listOf(box.item)) {}
        box.item.let { (val x: Int) -> }
    }
}

fun <<!CONFLICTING_UPPER_BOUNDS!>T<!>> shortFormBrackets(f: A, t: T, box: Box<out T>) where T : <!FINAL_UPPER_BOUND!>A<!>, T : I {
    if (true) {
        val [x: String] = <!COMPONENT_FUNCTION_MISSING!>f<!>
        for ([x: String] in <!COMPONENT_FUNCTION_MISSING!>listOf(f)<!>) {}
        f.let { <!COMPONENT_FUNCTION_MISSING!>[x: String]<!> -> }
    }
    if (true) {
        val [x: String] = <!COMPONENT_FUNCTION_MISSING!>J.makeFlexible(f)<!>
        for ([x: String] in <!COMPONENT_FUNCTION_MISSING!>listOf(J.makeFlexible(f))<!>) {}
        J.makeFlexible(f).let { <!COMPONENT_FUNCTION_MISSING!>[x: String]<!> -> }
    }
    if (<!IMPOSSIBLE_IS_CHECK_ERROR!>f is I<!>) {
        val [x: String] = f
        for ([x: String] in <!INFERRED_TYPE_VARIABLE_INTO_POSSIBLE_EMPTY_INTERSECTION!>listOf<!>(f)) {}
        f.<!INFERRED_TYPE_VARIABLE_INTO_POSSIBLE_EMPTY_INTERSECTION!>let<!> { [x: String] -> }
    }
    if (true) {
        val [x: String] = t
        for ([x: String] in listOf(t)) {}
        t.let { [x: String] -> }
    }
    if (true) {
        val [x: String] = box.item
        for ([x: String] in listOf(box.item)) {}
        box.item.let { [x: String] -> }
    }
}

fun <<!CONFLICTING_UPPER_BOUNDS!>T<!>> fullFormBrackets(f: A, t: T, box: Box<out T>) where T : <!FINAL_UPPER_BOUND!>A<!>, T : I {
    if (true) {
        [val x: String] = <!COMPONENT_FUNCTION_MISSING!>f<!>
        for ([val x: String] in <!COMPONENT_FUNCTION_MISSING!>listOf(f)<!>) {}
        f.let { <!COMPONENT_FUNCTION_MISSING!>[val x: String]<!> -> }
    }
    if (true) {
        [val x: String] = <!COMPONENT_FUNCTION_MISSING!>J.makeFlexible(f)<!>
        for ([val x: String] in <!COMPONENT_FUNCTION_MISSING!>listOf(J.makeFlexible(f))<!>) {}
        J.makeFlexible(f).let { <!COMPONENT_FUNCTION_MISSING!>[val x: String]<!> -> }
    }
    if (<!IMPOSSIBLE_IS_CHECK_ERROR!>f is I<!>) {
        [val x: String] = f
        for ([val x: String] in <!INFERRED_TYPE_VARIABLE_INTO_POSSIBLE_EMPTY_INTERSECTION!>listOf<!>(f)) {}
        f.<!INFERRED_TYPE_VARIABLE_INTO_POSSIBLE_EMPTY_INTERSECTION!>let<!> { [val x: String] -> }
    }
    if (true) {
        [val x: String] = t
        for ([val x: String] in listOf(t)) {}
        t.let { [val x: String] -> }
    }
    if (true) {
        [val x: String] = box.item
        for ([val x: String] in listOf(box.item)) {}
        box.item.let { [val x: String] -> }
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, getter, inheritanceDelegation, init, inner, integerLiteral, lambdaLiteral,
localClass, nullableType, primaryConstructor, propertyDeclaration, propertyDelegate, secondaryConstructor,
starProjection, stringLiteral, typeParameter, value */
