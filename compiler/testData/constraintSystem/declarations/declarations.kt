fun foo<T, P: T>() = 42

trait A
trait B : A
trait C : B

trait Consumer<in T>
trait Producer<out T>

trait My<T>