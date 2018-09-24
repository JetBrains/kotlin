// !WITH_NEW_INFERENCE
class ResolutionCandidate<A>

class ResolutionTask<B, C : B>(val candidate: ResolutionCandidate<B>)

fun <D, E : D> List<ResolutionTask<D, E>>.bar(t: ResolutionTask<D, E>) = t

public class ResolutionTaskHolder<F, G : F> {
    fun test(candidate: ResolutionCandidate<F>, tasks: MutableList<ResolutionTask<F, G>>) {
        tasks.bar(ResolutionTask<F, G>(candidate))
        tasks.add(ResolutionTask<F, G>(candidate))

        //todo the problem is the type of ResolutionTask is inferred as ResolutionTask<F, F> too early
        tasks.<!OI;TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS!>bar<!>(ResolutionTask(candidate))
        tasks.add(ResolutionTask(candidate))
    }
}