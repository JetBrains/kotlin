interface Iterator<out T> {
 fun next() : T
 val hasNext : Boolean

 fun <R> map(transform: (element: T) -> R) : Iterator<R> =
    object : Iterator<R> {
      override fun next() : R = transform(<!NO_THIS!>this@map<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>next<!>())

      override val hasNext : Boolean
        // There's no 'this' associated with the map() function, only this of the Iterator class
        get() = <!NO_THIS!>this@map<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>hasNext<!>
    }
}