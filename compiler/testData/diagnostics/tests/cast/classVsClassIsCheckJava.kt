// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-76766

// FILE: DefaultJavaClass.java
public class DefaultJavaClass { }

// FILE: FinalJavaClass.java
public final class FinalJavaClass { }

// FILE: JavaChild.java
public class JavaChild extends KotlinChild { }

// FILE: test.kt

import DefaultJavaClass
import FinalJavaClass
import JavaChild

open class KotlinClass
fun test1(a: DefaultJavaClass) = <!USELESS_IS_CHECK!>a is KotlinClass<!>

fun test2(a: FinalJavaClass) = a is <!INCOMPATIBLE_TYPES!>KotlinClass<!>

fun test3(a: KotlinClass) = <!USELESS_IS_CHECK!>a is DefaultJavaClass<!>

fun test4(a: KotlinClass) = a is <!INCOMPATIBLE_TYPES!>FinalJavaClass<!>

open class KotlinChild: DefaultJavaClass()

fun test5(a: KotlinChild) = <!USELESS_IS_CHECK!>a is DefaultJavaClass<!>

fun test6(a: DefaultJavaClass) = a is JavaChild

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, javaType */
