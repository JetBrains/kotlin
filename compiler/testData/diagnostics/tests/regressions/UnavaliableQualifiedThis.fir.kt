interface Iterator<out T> {
 fun next() : T
 val hasNext : Boolean

 fun <R> map(transform: (element: T) -> R) : Iterator<R> =
    object : Iterator<R> {
      override fun next() : R = transform(<!UNRESOLVED_LABEL!>this@map<!>.<!UNRESOLVED_REFERENCE!>next<!>())

      override val hasNext : Boolean
        // There's no 'this' associated with the map() function, only this of the Iterator class
        get() = <!UNRESOLVED_LABEL!>this@map<!>.<!UNRESOLVED_REFERENCE!>hasNext<!>
    }
}