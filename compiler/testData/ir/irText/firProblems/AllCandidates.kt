// FILE: OverloadResolutionResultsImpl.java

import java.util.*;

public class OverloadResolutionResultsImpl<D> {
    public Collection<ResolvedCall<D>> getAllCandidates() {
        return Collections.emptyList();
    }

    public void setAllCandidates(Collection<ResolvedCall<D>> allCandidates) {
    }

    public static <R> OverloadResolutionResultsImpl<R> nameNotFound() {
        OverloadResolutionResultsImpl<R> results = new OverloadResolutionResultsImpl<>();
        results.setAllCandidates(Collections.emptyList());
        return results;
    }
}

// FILE: AllCandidates.kt
// WITH_RUNTIME
// FULL_JDK

class ResolvedCall<C>

class MyCandidate(val resolvedCall: ResolvedCall<*>)

private fun <A> allCandidatesResult(allCandidates: Collection<MyCandidate>) =
    OverloadResolutionResultsImpl.nameNotFound<A>().apply {
        this.allCandidates = allCandidates.map {
            it.resolvedCall as ResolvedCall<A>
        }
    }
