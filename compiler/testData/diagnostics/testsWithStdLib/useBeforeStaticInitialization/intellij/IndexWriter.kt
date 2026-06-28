// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// WITH_STDLIB

<!POSSIBLE_INITIALIZATION_DEADLOCK!>abstract class IndexWriter {
    companion object {

        private val PARALLEL_WRITER_IMPL: String? = "IndexWriter.parallel.impl"

        <!POSSIBLY_UNINITIALIZED_PROPERTY!>private val defaultParallelWriter: ParallelIndexWriter = when (PARALLEL_WRITER_IMPL) {
            "FakeIndexWriter" -> <!ACCESSING_POSSIBLY_INACCESSIBLE_OBJECT_REFERENCE!>FakeIndexWriter<!>
            "ApplyViaCoroutinesWriter" -> <!CONSTRUCTING_POSSIBLY_DEADLOCKING_CLASS, CONSTRUCTING_POSSIBLY_DEADLOCKING_CLASS!>ApplyViaCoroutinesWriter()<!>
            "LegacyMultiThreadedIndexWriter" -> <!CONSTRUCTING_POSSIBLY_DEADLOCKING_CLASS, CONSTRUCTING_POSSIBLY_DEADLOCKING_CLASS!>LegacyMultiThreadedIndexWriter()<!>

            "MultiThreadedWithSuspendIndexWriter", null -> <!CONSTRUCTING_POSSIBLY_DEADLOCKING_CLASS, CONSTRUCTING_POSSIBLY_DEADLOCKING_CLASS!>MultiThreadedWithSuspendIndexWriter()<!>
            else -> <!CONSTRUCTING_POSSIBLY_DEADLOCKING_CLASS, CONSTRUCTING_POSSIBLY_DEADLOCKING_CLASS!>MultiThreadedWithSuspendIndexWriter()<!>
        }<!>

        init {
            <!ACCESSING_POSSIBLY_UNINITIALIZED_PROPERTY!>defaultParallelWriter<!>
        }

        @JvmStatic
        fun defaultParallelWriter(): ParallelIndexWriter = defaultParallelWriter
    }
}<!>

object SameThreadIndexWriter : IndexWriter()

<!POSSIBLE_INITIALIZATION_DEADLOCK!>abstract class ParallelIndexWriter(val workersCount: Int = 10) : IndexWriter()<!>

<!POSSIBLE_INITIALIZATION_DEADLOCK!>class LegacyMultiThreadedIndexWriter(workersCount: Int = 10) : ParallelIndexWriter(workersCount)<!>

<!POSSIBLE_INITIALIZATION_DEADLOCK!>object FakeIndexWriter<!> : ParallelIndexWriter()

<!POSSIBLE_INITIALIZATION_DEADLOCK!>class ApplyViaCoroutinesWriter(workersCount: Int = 10) : ParallelIndexWriter(workersCount)<!>

<!POSSIBLE_INITIALIZATION_DEADLOCK!>class MultiThreadedWithSuspendIndexWriter(workersCount: Int = 10) : ParallelIndexWriter(workersCount)<!>

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, disjunctionExpression, equalityExpression, functionDeclaration,
init, integerLiteral, nullableType, objectDeclaration, primaryConstructor, propertyDeclaration, stringLiteral,
whenExpression, whenWithSubject */
