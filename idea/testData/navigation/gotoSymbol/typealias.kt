typealias testGlobal = Any

fun some() {
    typealias testInFun = Any
}

interface SomeTrait {
    typealias testInTrait = Any
}

class Some() {
    typealias testInClass = Any

    companion object {
        typealias testInClassObject = Any
    }
}

// SEARCH_TEXT: test
// REF: typealias testGlobal = Any
// REF: typealias testInClass = Any
// REF: typealias testInClassObject = Any
// REF: typealias testInTrait = Any
