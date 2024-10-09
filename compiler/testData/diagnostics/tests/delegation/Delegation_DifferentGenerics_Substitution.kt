// ISSUE: KT-46120, KT-72140
// WITH_STDLIB
// RENDER_DIAGNOSTICS_FULL_TEXT
// LANGUAGE: -ForbidImplementationByDelegationWithDifferentGenericSignature

// FILE: JI.java

import java.util.List;

public interface JI<T> {
    <C> List<T> foo();

    <C> List<C> bar();
}

// FILE: JC.java

import java.util.List;

public class JC implements JI<String> {
    public List<String> foo() {
        return null;
    }

    public List<String> bar() {
        return null;
    }
}

// FILE: test.kt

class C1(client: JC) : JI<Int> by <!TYPE_MISMATCH!>client<!>

class C2(client: JC) : JI<String> by client
