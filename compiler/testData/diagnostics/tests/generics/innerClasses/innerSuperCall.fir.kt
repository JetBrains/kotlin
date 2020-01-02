open class Super<T> {
    inner open class Inner {
    }
}

class Sub : Super<String>() {
    // TODO: it would be nice to have a possibility to omit explicit type argument in supertype
    inner class SubInner : Super<String>.Inner() {}
}
