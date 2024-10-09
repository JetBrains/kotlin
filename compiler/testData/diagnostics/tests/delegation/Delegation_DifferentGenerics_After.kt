// ISSUE: KT-46120, KT-72140
// WITH_STDLIB
// RENDER_DIAGNOSTICS_FULL_TEXT
// LANGUAGE: +ForbidImplementationByDelegationWithDifferentGenericSignature

// FILE: JI.java

import java.util.List;

public interface JI {
    <C> List<C> foo();

    <D> D baz();

    <E, F> List<F> bar();
}

// FILE: JC.java

import java.util.List;

public class JC implements JI {
    public List<String> foo() {
        return null;
    }

    public Object baz() {
        return null;
    }

    public List<Integer> bar() {
        return null;
    }
}

// FILE: JKC.java

import java.util.List;

public class JKC implements KI {
    public List<String> foo() {
        return null;
    }

    public Object baz() {
        return null;
    }

    public List<Integer> bar() {
        return null;
    }
}

// FILE: KI.kt

interface KI {
    fun <C> foo(): List<C>

    fun <D> baz(): D

    fun <E, F> bar(): List<F>
}

// FILE: KC.kt

class KC : KI {
    <!CONFLICTING_OVERLOADS!><!NOTHING_TO_OVERRIDE!>override<!> fun foo(): List<String><!> = emptyList()

    <!CONFLICTING_OVERLOADS!><!NOTHING_TO_OVERRIDE!>override<!> fun baz(): Any<!> = 42

    <!CONFLICTING_OVERLOADS!><!NOTHING_TO_OVERRIDE!>override<!> fun bar(): List<Int><!> = listOf(0)
}

// FILE: KJC.kt

class KJC : JI {
    <!CONFLICTING_OVERLOADS!><!NOTHING_TO_OVERRIDE!>override<!> fun foo(): List<String><!> = emptyList()

    <!CONFLICTING_OVERLOADS!><!NOTHING_TO_OVERRIDE!>override<!> fun baz(): Any<!> = 42

    <!CONFLICTING_OVERLOADS!><!NOTHING_TO_OVERRIDE!>override<!> fun bar(): List<Int><!> = listOf(0)
}

// FILE: test.kt

class C1(client: JC) : JI by client

class C2(client: KC) : KI by client

class C3(client: KJC) : JI by client

class C4(client: JKC) : KI by client
