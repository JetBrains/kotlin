// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

// FILE: KotlinBase.kt

@MustUseReturnValue
interface KotlinBase {
    fun x(): String
    @IgnorableReturnValue fun ign(): String
    val p: String
}

// FILE: JavaI1.java

public interface JavaI1 extends KotlinBase {
    String x();
    String ign();
    String getP();
}

// FILE: JavaI2.java

import kotlin.IgnorableReturnValue;

public interface JavaI2 extends KotlinBase {
    @IgnorableReturnValue
    String x();
    String ign();
    String getP();
}

// FILE: KtFile.kt

class K1: JavaI1 {
    override fun x() = ""
    override fun ign() = ""
    override val p = ""
}

class K2: JavaI2 {
    override fun x() = ""
    override fun ign() = ""
    override val p = ""
}

fun testX(b: KotlinBase, j1: JavaI1, j2: JavaI2, k1: K1, k2: K2) {
    b.x()
    j1.x()
    k1.x()
    j2.x()
    k2.x()
}

fun testP(b: KotlinBase, j1: JavaI1, j2: JavaI2, k1: K1, k2: K2) {
    b.p
    j1.p
    k1.p
    j2.p
    k2.p
}

fun testIgn(b: KotlinBase, j1: JavaI1, j2: JavaI2, k1: K1, k2: K2) {
    b.ign()
    j1.ign()
    k1.ign()
    j2.ign()
    k2.ign()
}

fun testToString(b: KotlinBase, j1: JavaI1, j2: JavaI2, k1: K1, k2: K2) {
    b.toString()
    j1.toString()
    k1.toString()
    j2.toString()
    k2.toString()
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, interfaceDeclaration, javaFunction, javaType, override,
stringLiteral */
