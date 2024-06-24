// FIR_IDENTICAL
// LANGUAGE: +ExpectedUnitAsSoftConstraint

fun onSetVersion(underlying: () -> Unit): () -> Unit {
    return {
        trackPlainOperation(
            { SetVersion() },
            { underlying() }
        )
    }
}

fun <R : Any> trackPlainOperation(
    composeOperation: (OperationResult<R>) -> VfsOperation<R>,
    performOperation: () -> R,
): R = TODO()

abstract class VfsOperation<T : Any>
abstract class RecordsOperation<T : Any> : VfsOperation<T>()
class SetVersion : RecordsOperation<Unit>()

class OperationResult<out T : Any>