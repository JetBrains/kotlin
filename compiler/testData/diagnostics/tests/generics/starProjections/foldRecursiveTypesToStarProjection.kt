// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_EXPRESSION
// WITH_STDLIB

object KT32183 {
    interface AGraphExecutionEntity {
        val jobs: Sequence<AJobExecutionEntity>
    }

    interface AJobExecutionEntity {
        val meta: ProjectJob.Process<*, *>
    }

    sealed class ProjectJob {
        sealed class Process<E : ProcessExecutable<E>, R : ProcessResources<R>> : ProjectJob()
        sealed class ProcessExecutable<E : ProcessExecutable<E>>
        sealed class ProcessResources<R : ProcessResources<R>>
    }

    fun test(graph: AGraphExecutionEntity) {
        val statusByMeta = graph.jobs.associateBy { it.meta }
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<KT32183.ProjectJob.Process<*, *>, KT32183.AJobExecutionEntity>")!>statusByMeta<!>
    }
}

object KT31474 {
    abstract class A<T : A<T>>
    class B : A<B>()
    class C : A<C>()

    fun test() {
        val a = listOf(B(), C())
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.List<KT31474.A<*>>")!>a<!>
    }
}

object KT31853 {
    interface A<out T>
    interface B : A<B>
    interface C : A<C>

    fun test(b: B, c: C) {
        val a = if (true) b else c
        <!DEBUG_INFO_EXPRESSION_TYPE("KT31853.A<*>")!>a<!>
    }
}