// SKIP_KT_DUMP
// TARGET_BACKEND: JVM

// FILE: Jaba.java
public class Jaba<T> {
    public void foo(T f) {}
}

// FILE: main.kt
class A : Jaba<Int?>(), I2 { // Duplicated foo's
}

class B : Jaba<Int>(), I2 { // Duplicated foo's
}

class C : Jaba<String>(), KotlinInterface // Collapsed foo

abstract class D : Jaba<String>(), KotlinInterface // Collapsed foo

abstract class E : Jaba<String?>(), KotlinInterface // Mistakenly duplicated foo's: KT-82502

class F : Jaba<String?>(), KotlinInterface2 // Mistakenly duplicated foo's: KT-82502

class G : Jaba<String?>(), KotlinInterface2 { // Mistakenly duplicated foo's: KT-82502
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
