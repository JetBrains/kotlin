object A<T>
object B<in T, out R>
object C<T : Comparable<T>>

class D {
    companion object<T>
}

class E {
    companion object<in T, out R>
}

class F {
    companion object C<T : Comparable<T>>
}

class G {
    companion object F<T>
}

object H<T, R>()
