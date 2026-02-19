class A<T>

class B<T1, T2>

class C {
    fun foo<caret>() = object : B<String, A<Unknown>> {

    }
}
