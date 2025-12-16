// TARGET_BACKEND: JVM

// FILE: main.kt
public open class B {
    fun plus(a: Any): String = ""
    infix fun infixFun(a: Any): String = ""
}

public interface D {
    fun plus(a: Any): Any
    infix fun infixFun(a: Any): Any
}

public interface C {
    operator fun plus(a: Any): CharSequence
    infix fun infixFun(a: Any): CharSequence
}

class A1 : B(), C, D

class A2 : B(), D, C

class A3 : B(), C

class A4 : B(), D

interface Kjk : Jaba

// FILE: Jaba.java
public interface Jaba extends C {
    public CharSequence plus(Object a);
    public CharSequence infixFun(Object a);
}
