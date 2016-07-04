typealias TestGlobal = Any

fun some() {
    typealias TestInFun = Any
}

interface SomeTrait {
    typealias TestInTrait = Any
}

class Some() {
    typealias TestInClass = Any

    companion object {
        typealias TestInClassObject = Any
    }
}

// SEARCH_TEXT: Test
// REF: typealias TestGlobal = Any
// REF: typealias TestInClass = Any
// REF: typealias TestInClassObject = Any
// REF: typealias TestInTrait = Any
