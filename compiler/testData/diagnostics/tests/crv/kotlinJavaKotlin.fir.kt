// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

// FILE: KotlinBase.kt

@MustUseReturnValue
interface KotlinBase {
    fun x(): String
    @IgnorableReturnValue fun ign(): String
}

// FILE: JavaI1.java

public interface JavaI1 extends KotlinBase {
    String x();
    String ign();
}

// FILE: JavaI2.java

import kotlin.IgnorableReturnValue;

public interface JavaI2 extends KotlinBase {
    @IgnorableReturnValue
    String x();
    String ign();
}

// FILE: KtFile.kt

class K1: JavaI1 {
    override fun x() = ""
    override fun ign() = ""
}

class K2: JavaI2 {
    override fun x() = ""
    override fun ign() = ""
}

fun testX(b: KotlinBase, j1: JavaI1, j2: JavaI2, k1: K1, k2: K2) {
    <!RETURN_VALUE_NOT_USED!>b.x()<!>
    <!RETURN_VALUE_NOT_USED!>j1.x()<!>
    <!RETURN_VALUE_NOT_USED!>k1.x()<!>
    j2.x()
    k2.x()
}

fun testIgn(b: KotlinBase, j1: JavaI1, j2: JavaI2, k1: K1, k2: K2) {
    b.ign()
    j1.ign()
    k1.ign()
    j2.ign()
    k2.ign()
}

fun testToString(b: KotlinBase, j1: JavaI1, j2: JavaI2, k1: K1, k2: K2) {
    <!RETURN_VALUE_NOT_USED!>b.toString()<!>
    <!RETURN_VALUE_NOT_USED!>j1.toString()<!>
    <!RETURN_VALUE_NOT_USED!>k1.toString()<!>
    <!RETURN_VALUE_NOT_USED!>j2.toString()<!>
    <!RETURN_VALUE_NOT_USED!>k2.toString()<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, interfaceDeclaration, javaFunction, javaType, override,
stringLiteral */
