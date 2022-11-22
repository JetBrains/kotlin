// KT-54028

// FILE: file1.kt

sealed interface LazyGridLayoutInfo {
    fun ok(): String
}

// FILE: file2.kt

class LazyGridState {
    val layoutInfo: LazyGridLayoutInfo
        get() = EmptyLazyGridLayoutInfo
}

private object EmptyLazyGridLayoutInfo : LazyGridLayoutInfo {
    override fun ok() = "OK"
}

// FILE: main.kt

fun box(): String {
    return LazyGridState().layoutInfo.ok()
}

