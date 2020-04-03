/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: annotations, annotation-classes
 * NUMBER: 1
 * DESCRIPTION: Annotation classes with forbidden `JvmOverloads` annotation on its constructor.
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-25702
 */

// TESTCASE NUMBER: 2, 3, 4
@Target(AnnotationTarget.CONSTRUCTOR)
annotation class Case12_1

@Target(AnnotationTarget.CONSTRUCTOR)
@Retention(AnnotationRetention.SOURCE)
@Repeatable
annotation class Case12_2

// TESTCASE NUMBER: 1
annotation class Case1 <!OVERLOADS_ANNOTATION_CLASS_CONSTRUCTOR!>@JvmOverloads<!> constructor(val x: Int)

// TESTCASE NUMBER: 2
annotation class Case2 @[Case12_2 Case12_2 Case12_2 Case12_2 Case12_2 Case12_2 <!OVERLOADS_ANNOTATION_CLASS_CONSTRUCTOR!>JvmOverloads<!> Case12_1] constructor(<!MISSING_VAL_ON_ANNOTATION_PARAMETER!>x: Int = 10<!>)

// TESTCASE NUMBER: 3
annotation class Case3 @Case12_2 @Case12_1 @Case12_2 <!OVERLOADS_ANNOTATION_CLASS_CONSTRUCTOR!>@`JvmOverloads`<!> @Case12_2 @Case12_2 constructor(<!MISSING_VAL_ON_ANNOTATION_PARAMETER!>x: Int = 10<!>)

// TESTCASE NUMBER: 4
annotation class Case4 @Case12_2 @[Case12_2 Case12_2 Case12_2 Case12_2 Case12_2 Case12_2 <!OVERLOADS_ANNOTATION_CLASS_CONSTRUCTOR!>JvmOverloads<!> Case12_1] @Case12_2 @Case12_2 constructor(<!MISSING_VAL_ON_ANNOTATION_PARAMETER!>x: Int = 10<!>)

// TESTCASE NUMBER: 5
annotation class Case5 @[<!OVERLOADS_ANNOTATION_CLASS_CONSTRUCTOR!>JvmOverloads<!>] constructor(val x: Int)

// TESTCASE NUMBER: 6
annotation class Case6 <!OVERLOADS_ANNOTATION_CLASS_CONSTRUCTOR!>@JvmOverloads<!> constructor()

// TESTCASE NUMBER: 7
annotation class Case7<!OVERLOADS_ANNOTATION_CLASS_CONSTRUCTOR!>@JvmOverloads<!> constructor() {}

// TESTCASE NUMBER: 8
annotation class Case8 <!OVERLOADS_ANNOTATION_CLASS_CONSTRUCTOR!>@`JvmOverloads`<!> constructor()

// TESTCASE NUMBER: 9
annotation class Case9 <A, Case12_1 : A, Case12_2 : Case12_1, D : Case12_2, E : D> <!OVERLOADS_ANNOTATION_CLASS_CONSTRUCTOR!>@JvmOverloads<!> constructor(val x: Int)

// TESTCASE NUMBER: 10
annotation class Case10 <T> <!OVERLOADS_ANNOTATION_CLASS_CONSTRUCTOR!>@JvmOverloads<!> constructor(val x: Int)

// TESTCASE NUMBER: 11
annotation class Case11 <T : Number, K : Comparable<K>> <!OVERLOADS_ANNOTATION_CLASS_CONSTRUCTOR!>@JvmOverloads<!> constructor(val x: Int)

// TESTCASE NUMBER: 12
annotation class Case12 <T> <!OVERLOADS_ANNOTATION_CLASS_CONSTRUCTOR!>@JvmOverloads<!> constructor(val x: Int) where T: Number

// TESTCASE NUMBER: 13
annotation class Case13 constructor(val x: Int) {
    annotation class Case1 <!OVERLOADS_ANNOTATION_CLASS_CONSTRUCTOR!>@JvmOverloads<!> constructor(val x: Int)
}

// TESTCASE NUMBER: 14
annotation class Case14 <!OVERLOADS_ANNOTATION_CLASS_CONSTRUCTOR!>@JvmOverloads<!> constructor(val x: Int) {
    annotation class Case1 constructor(val x: Int)
}

// TESTCASE NUMBER: 15
object Case15 {
    annotation class Case15 <!OVERLOADS_ANNOTATION_CLASS_CONSTRUCTOR!>@JvmOverloads<!> constructor(val x: Int)
}

// TESTCASE NUMBER: 16
class Case16 {
    annotation class Case16 <!OVERLOADS_ANNOTATION_CLASS_CONSTRUCTOR!>@JvmOverloads<!> constructor(val x: Int)
}
