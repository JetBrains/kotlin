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
fun test1(a: DefaultJavaClass) = <!IMPOSSIBLE_IS_CHECK_ERROR!>a is KotlinClass<!>

fun test2(a: FinalJavaClass) = <!IMPOSSIBLE_IS_CHECK_ERROR!>a is KotlinClass<!>

fun test3(a: KotlinClass) = <!IMPOSSIBLE_IS_CHECK_ERROR!>a is DefaultJavaClass<!>

fun test4(a: KotlinClass) = <!IMPOSSIBLE_IS_CHECK_ERROR!>a is FinalJavaClass<!>

open class KotlinChild: DefaultJavaClass()

fun test5(a: KotlinChild) = <!USELESS_IS_CHECK!>a is DefaultJavaClass<!>

fun test6(a: DefaultJavaClass) = a is JavaChild

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, javaType */
