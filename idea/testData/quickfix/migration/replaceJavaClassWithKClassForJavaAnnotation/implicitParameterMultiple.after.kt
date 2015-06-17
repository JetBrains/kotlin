import java.util.Random

// "Replace javaClass<T>() with T::class" "true"
// WITH_RUNTIME

Ann(A::class, A::class, *array(A::class), arg1 = A.B::class, arg2 = Random::class) class MyClass
