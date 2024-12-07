// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-46120, KT-72140
// WITH_STDLIB

// FILE: JI.java

import java.util.List;

public interface JI {
    List foo();
    List<Integer> bar();
    <C> List<C> baz();
}

// FILE: JC.java

import java.util.List;

public class JC implements JI {
    @Override
    public List<String> foo() {
        return null;
    }
    @Override
    public List bar(){
        return null;
    }

    @Override
    public <C> List baz() {
        return null;
    }
}

// FILE: JKC.java

import java.util.List;

public class JKC implements KI {
    @Override
    public List foo() {
        return null;
    }

    @Override
    public <T> List bar() {
        return null;
    }
}

// FILE: KI.kt

interface KI {
    fun foo(): List<Int>
    fun <T> bar(): List<T>
}

// FILE: test.kt

class C: JI by JC()

class C2: KI by JKC()