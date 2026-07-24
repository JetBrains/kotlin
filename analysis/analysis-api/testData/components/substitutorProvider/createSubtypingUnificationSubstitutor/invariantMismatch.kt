interface A<T> : B<T>
interface B<T>

open class Animal
class Dog : Animal()

fun <T: Dog> test(yy: A<T>, xx: B<Animal>) {
    y<caret_1_left>y
    x<caret_1_right>x
}
