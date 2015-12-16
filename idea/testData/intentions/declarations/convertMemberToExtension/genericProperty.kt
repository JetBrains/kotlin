// ERROR: Property must be initialized or be abstract
// ERROR: Type parameter of a property must be used in its receiver type
// WITH_RUNTIME
// SKIP_ERRORS_AFTER

class Owner<T> {
    val <R> <caret>p: R
}
