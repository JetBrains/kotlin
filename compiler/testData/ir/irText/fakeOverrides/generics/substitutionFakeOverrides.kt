// TARGET_BACKEND: JVM

// The test primarily tests reflect dumps (k1 vs new reflect), we don't need kt dumps
// SKIP_KT_DUMP

// Skip kotlin-reflect because K1 is wrong: KT-82502
// KOTLIN_REFLECT_DUMP_MISMATCH

// FILE: Jaba.java
public class Jaba<T> {
    public void foo(T f) {}
}

// FILE: main.kt
class A : Jaba<Int?>(), I2 {
}

class B : Jaba<Int>(), I2 {
}

class C : Jaba<String>(), KotlinInterface

abstract class D : Jaba<String>(), KotlinInterface

abstract class E : Jaba<String?>(), KotlinInterface // Mistakenly duplicated foo's in K1: KT-82502

class F : Jaba<String?>(), KotlinInterface2 // Mistakenly duplicated foo's in K1: KT-82502

class G : Jaba<String?>(), KotlinInterface2 { // Mistakenly duplicated foo's in K1: KT-82502
    override fun foo(t: String) {}
}

interface I2 {
    fun foo(f: Int) {}
}

interface KotlinInterface {
    fun foo(t: String)
}

interface KotlinInterface2 {
    fun foo(t: String) {}
}
