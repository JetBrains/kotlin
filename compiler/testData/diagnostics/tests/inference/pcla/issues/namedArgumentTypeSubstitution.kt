// FIR_IDENTICAL
// WITH_STDLIB

interface SortableItem

interface WeightedBatchSourceProvider<out TItem : SortableItem, TCursor>

interface BatchSourceAggregator<out T : SortableItem> {
    val elements: Property<List<T>>
}

interface Property<out T> {
    val value: T
}

interface XFilteredListStateOnBatchSourceAggregator<out T : SortableItem>

interface BatchSource<T>

class WeightedBatchSourceOverProvider<TItem : SortableItem, TCursor> : BatchSource<TItem>

fun searchDialogListState2(
    sources: List<WeightedBatchSourceProvider<SortableItem, String>>,
) = xFilteredListStateOnBatchSourceAggregator2(
) { prev ->
    val initialElements = prev?.elements?.value

    batchSourceAggregator2(
        buckets = sources
            .map { weightedBatchSource2(
                it,
            ) },
        initialElements = initialElements ?: emptyList(),
    )
}

fun <T : SortableItem> xFilteredListStateOnBatchSourceAggregator2(
    aggregatorSource: suspend (BatchSourceAggregator<T>?) -> BatchSourceAggregator<T>,
): XFilteredListStateOnBatchSourceAggregator<T> = TODO()

fun <TTag> weightedBatchSource2(
    provider: WeightedBatchSourceProvider<SortableItem, TTag>,
): WeightedBatchSourceOverProvider<SortableItem, TTag> = TODO()

fun <T : SortableItem> batchSourceAggregator2(
    buckets: List<BatchSource<out T>>,
    initialElements: List<T> = emptyList(),
): BatchSourceAggregator<T> = TODO()