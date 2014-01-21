fun foo<T, P, Q, R, S>() = 42

trait A
trait B : A
trait C : B

trait Consumer<in T>
trait Producer<out T>

trait My<T>