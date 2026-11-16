// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-89779
// DIAGNOSTICS: -UNUSED_PARAMETER
// FILE: other.kt
package ppp

private val invisible: Int = 1

// FILE: main.kt
package ppp

fun single(i: Int) = i
fun intOrString(i: Int) = i
fun intOrString(s: String) = s
fun intOrAny(i: Int) = i
fun intOrAny(a: Any) = a

class A {
    val prop = 1
    constructor(x: Int)
    constructor(x: Int, y: Int) : this(single(<!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>prop<!>))
    constructor(x: Int, y: Int, z: Int) : this(<!NONE_APPLICABLE!>intOrString<!>(<!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>prop<!>))
    constructor(x: Int, y: Int, z: Int, w: Int) : this(intOrAny(<!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>prop<!>))
    constructor(x: Int, y: Int, z: Int, w: Int, v: Int) : this(x <!NONE_APPLICABLE!>+<!> <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>prop<!>)
    constructor(x: Int, y: Int, z: Int, w: Int, v: Int, u: Int = x <!NONE_APPLICABLE!>+<!> <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>prop<!>) : this(x)
}

fun test() {
    single(<!INVISIBLE_REFERENCE!>invisible<!>)
    <!NONE_APPLICABLE!>intOrString<!>(<!INVISIBLE_REFERENCE!>invisible<!>)
    single(<!UNRESOLVED_REFERENCE!>unresolved<!>)
    intOrString(<!UNRESOLVED_REFERENCE!>unresolved<!>)
}

/* GENERATED_FIR_TAGS: additiveExpression, classDeclaration, functionDeclaration, integerLiteral, propertyDeclaration,
secondaryConstructor */
