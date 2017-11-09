// "Initialize with constructor parameter" "true"
// WITH_RUNTIME
abstract class Form<T>(val name: String){
    var <caret>data: T?
        set(value){
            value?.let { processData(it) }
            field = data
        }

    abstract protected fun processData(data: T)
}