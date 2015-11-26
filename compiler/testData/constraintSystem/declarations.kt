fun <T, P, E> foo() = 42

interface A
interface B : A
interface C : B

interface Consumer<in T>
interface Producer<out T>
interface Inv<T>

interface My<T>
interface Successor<T> : My<T>

interface Two<T, P>
interface Fun<in T, out R>