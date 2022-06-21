// FIR_IDENTICAL
// WITH_STDLIB

interface Task

interface TaskCollection<T : Task> {
    fun <S : T> withType(type: Class<S>): TaskCollection<S>
}

internal inline fun <reified S : Task> TaskCollection<in S>.withType(): TaskCollection<S> = withType(S::class.java)
