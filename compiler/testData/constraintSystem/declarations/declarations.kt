fun foo<T, P: T>() = 42

interface A
interface B : A
interface C : B

interface Consumer<in T>
interface Producer<out T>

interface My<T>