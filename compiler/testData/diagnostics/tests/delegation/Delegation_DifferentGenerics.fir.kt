// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-46120, KT-72140
// WITH_STDLIB
// RENDER_DIAGNOSTICS_FULL_TEXT
// LANGUAGE: -ForbidImplementationByDelegationWithDifferentGenericSignature

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

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class KC<!> : KI {
    <!NOTHING_TO_OVERRIDE!>override<!> <!CONFLICTING_OVERLOADS!>fun foo(): List<String><!> = emptyList()

    <!NOTHING_TO_OVERRIDE!>override<!> <!CONFLICTING_OVERLOADS!>fun baz(): Any<!> = 42

    <!NOTHING_TO_OVERRIDE!>override<!> <!CONFLICTING_OVERLOADS!>fun bar(): List<Int><!> = listOf(0)
}

// FILE: KJC.kt

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class KJC<!> : JI {
    <!NOTHING_TO_OVERRIDE!>override<!> <!CONFLICTING_OVERLOADS!>fun foo(): List<String><!> = emptyList()

    <!NOTHING_TO_OVERRIDE!>override<!> <!CONFLICTING_OVERLOADS!>fun baz(): Any<!> = 42

    <!NOTHING_TO_OVERRIDE!>override<!> <!CONFLICTING_OVERLOADS!>fun bar(): List<Int><!> = listOf(0)
}

// FILE: test.kt

class C1(client: JC) : <!IMPLEMENTATION_BY_DELEGATION_WITH_DIFFERENT_GENERIC_SIGNATURE_WARNING, IMPLEMENTATION_BY_DELEGATION_WITH_DIFFERENT_GENERIC_SIGNATURE_WARNING, IMPLEMENTATION_BY_DELEGATION_WITH_DIFFERENT_GENERIC_SIGNATURE_WARNING!>JI<!> by client

class C2(client: KC) : KI by client

class C3(client: KJC) : JI by client

class C4(client: JKC) : <!IMPLEMENTATION_BY_DELEGATION_WITH_DIFFERENT_GENERIC_SIGNATURE_WARNING, IMPLEMENTATION_BY_DELEGATION_WITH_DIFFERENT_GENERIC_SIGNATURE_WARNING, IMPLEMENTATION_BY_DELEGATION_WITH_DIFFERENT_GENERIC_SIGNATURE_WARNING!>KI<!> by client

class C5(client: JC) : JI by client {
    override fun <C> foo(): List<C> {
        return emptyList()
    }

    override fun <E, F> bar(): List<F> {
        return emptyList()
    }

    override fun <D> baz(): D {
        return null!!
    }
}

class C6(client: JKC) : KI by client{
    override fun <C> foo(): List<C> {
        return null!!
    }
    override fun <E, F> bar(): List<F> {
        return null!!
    }
    override fun <D> baz(): D {
        return null!!
    }
}