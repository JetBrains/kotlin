import java.util.LinkedHashSet

class A {
    fun <T> bar(root: List<T>, list: LinkedHashSet<List<T>>) {
        addIfNotNull(root, list)
    }

    private fun <T> <caret>addIfNotNull(element: T, result: Collection<T>) {
        nested(result, element)
    }


    private fun <S> nested(result: Collection<S>, element: S) {}
}