// JAVAC_EXPECTED_FILE
// FILE: MyMap.java

import java.util.AbstractMap;
import java.util.Set;

class MyMap<K, V> extends AbstractMap<K, V> {
    @Override
    public Set<Entry<K, V>> entrySet() {
        return null;
    }
}

// FILE: main.kt

interface ResolverForProject<M1> {
    val exposeM: M1 get() = null!!
}

class ResolverForProjectImpl<M>(
        <!UNUSED_PARAMETER!>descriptorByModule<!>: Map<M, String>,
        <!UNUSED_PARAMETER!>delegateResolver<!>: ResolverForProject<M>
) : ResolverForProject<M>

interface WithFoo {
    fun foo()
}

fun <M2: WithFoo> foo(delegateResolver: ResolverForProject<M2?>): ResolverForProject<M2?> {
    val descriptorByModule = MyMap<M2, String>()
    val result = ResolverForProjectImpl(descriptorByModule, delegateResolver)
    result.exposeM.foo() // M is not M2?
    result.exposeM?.foo() // no warning, M is not M2, hense M is M2!

    return ResolverForProjectImpl(descriptorByModule, delegateResolver) // another bound check
}

// MyMap<M2, String> :< Map<M, String> => M = M2!
// RFP<M2?> :< RFP<M> => M = M2?