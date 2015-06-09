// "Replace javaClass<T>() with T::class in whole project" "true"
// WITH_RUNTIME

Ann(String::class, arg = Int::class, args = array()) class MyClass1

Ann(String::class, arg = Int::class, x = 1, args = array(Double::class)) class MyClass2 {
    Ann(String::class, arg = Int::class, args = array(Double::class)) class Nested {
        Ann(String::class, arg = Int::class, args = array(Double::class)) fun foo1() {

            @Ann(String::class, arg = Int::class, args = array(Double::class)) class Local
        }

        @Ann(String::class, arg = Int::class, args = array(Double::class), x = 1) fun foo2() {

            @Ann(String::class, arg = Int::class, args = array(Double::class)) val local = 0
        }
    }

    inner Ann(Double::class) class Inner
}
