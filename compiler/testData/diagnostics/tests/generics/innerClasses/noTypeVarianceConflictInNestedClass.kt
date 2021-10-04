// FIR_IDENTICAL
// KT-49078

class CidrMemoryData<T> {
    interface Data<out T>
    abstract class AbstractData<out T, E : Data<T>>
}