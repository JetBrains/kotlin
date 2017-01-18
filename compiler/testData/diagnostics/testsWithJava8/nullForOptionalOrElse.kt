import java.util.*
import java.util.stream.Stream

fun <T> Stream<T>?.getIfSingle() =
        this?.map { Optional.ofNullable(it) }
        ?.reduce(Optional.empty()) { _, _ -> Optional.empty() }
        ?.orElse(null) // <<---- should not be an error
