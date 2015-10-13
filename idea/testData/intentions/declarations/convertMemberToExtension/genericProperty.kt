// ERROR: Property must be initialized or be abstract
// ERROR: Type parameter of a property must be used in its receiver type

class Owner<T> {
    val <R> <caret>p: R
}
