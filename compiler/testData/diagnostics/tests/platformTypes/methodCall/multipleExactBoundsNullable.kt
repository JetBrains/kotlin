import java.util.HashMap

trait ResolverForProject<M1> {
    val exposeM: M1 get() = null!!
}

class ResolverForProjectImpl<M>(
        descriptorByModule: Map<M, String>,
        delegateResolver: ResolverForProject<M>
) : ResolverForProject<M>

trait WithFoo {
    fun foo()
}

fun <M2: WithFoo> foo(delegateResolver: ResolverForProject<M2?>): ResolverForProject<M2> {
    val descriptorByModule = HashMap<M2, String>()
    val result = ResolverForProjectImpl(descriptorByModule, delegateResolver)
    result.exposeM.foo() // M is not M2?
    result.exposeM?.foo() // no warning, M is not M2, hense M is M2!

    return ResolverForProjectImpl(descriptorByModule, delegateResolver) // another bound check
}

// HashMap<M2, String> :< Map<M, String> => M = M2!
// RFP<M2?> :< RFP<M> => M = M2?