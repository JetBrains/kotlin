package test

interface Base<T> {
    var value: T
}

class Derived : Base<String>()

// setter: callable: test/Derived.value
