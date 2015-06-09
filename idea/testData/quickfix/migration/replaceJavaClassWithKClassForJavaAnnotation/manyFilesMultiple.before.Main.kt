// "Replace javaClass<T>() with T::class in whole project" "true"
// WITH_RUNTIME

Ann(javaClass<String>()<caret>, arg = javaClass<Int>(), args = array()) class MyClass1

Ann(javaClass<String>(), arg = javaClass<Int>(), x = 1, args = array(javaClass<Double>())) class MyClass2 {
    Ann(javaClass<String>(), arg = javaClass<Int>(), args = array(javaClass<Double>())) class Nested {
        Ann(javaClass<String>(), arg = javaClass<Int>(), args = array(javaClass<Double>())) fun foo1() {

            @Ann(javaClass<String>(), arg = javaClass<Int>(), args = array(javaClass<Double>())) class Local
        }

        @Ann(javaClass<String>(), arg = javaClass<Int>(), args = array(javaClass<Double>()), x = 1) fun foo2() {

            @Ann(javaClass<String>(), arg = javaClass<Int>(), args = array(javaClass<Double>())) val local = 0
        }
    }

    inner Ann(javaClass<Double>()) class Inner
}
