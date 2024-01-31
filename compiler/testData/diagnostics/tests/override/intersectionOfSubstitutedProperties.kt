// FIR_IDENTICAL

abstract class C<T, ModelPropertyT : ModelListPropertyCore<T>>() :
    A<ModelPropertyT, T>(),
    I<List<T>>

abstract class A<out ModelPropertyT : ModelPropertyCore<*>, T> {
    abstract val property: ModelPropertyT
}

interface I<out T> {
    val property: ModelPropertyCore<out T>
}

interface ModelListPropertyCore<T> : ModelPropertyCore<List<T>>
interface ModelPropertyCore<T>