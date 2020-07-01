class A

class OuterClass {
    fun A.innerExt() {}

    fun test(a: A) {
        a.<caret>
    }
}

// EXIST: innerExt