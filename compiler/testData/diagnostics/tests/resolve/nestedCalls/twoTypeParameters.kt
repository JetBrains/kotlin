class ResolutionCandidate<A>

class ResolutionTask<B, C : B>(val candidate: ResolutionCandidate<B>)

fun <D, E : D> List<ResolutionTask<D, E>>.bar(t: ResolutionTask<D, E>) = t

public class ResolutionTaskHolder<F, G : F> {
    fun test(candidate: ResolutionCandidate<F>, tasks: MutableList<ResolutionTask<F, G>>) {
        tasks.bar(ResolutionTask(candidate))
    }
}

