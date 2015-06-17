import java.lang

// "Replace javaClass<T>() with T::class" "true"
// WITH_RUNTIME

Ann(
        String::class,
        Int::class,
*array(Double::class),
x = 2,
arg = Int::class,
args = array(Any::class, lang.String::class))
class MyClass
