interface A

object Some1: A {

}

object Some2: A

object Some3

    :


        A

val a = object: A {}

val b = object: A {}

class B {
    companion object: A {

    }
}

class C {
    companion object:
            A {

    }
}

// SET_TRUE: SPACE_BEFORE_EXTEND_COLON
// SET_FALSE: SPACE_AFTER_EXTEND_COLON
