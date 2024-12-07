//ALLOW_AST_ACCESS
package test

class ConstructorTypeParamClassObjectTypeConflict<test> {
    companion object {
        interface test
    }

    val some: test? = throw Exception()
}

class ConstructorTypeParamClassObjectConflict<test> {
    companion object {
        val test = { 12 }()
    }

    val some = test
}

class TestConstructorParamClassObjectConflict(test: String) {
    companion object {
        val test = { 12 }()
    }

    val some = test
}


class TestConstructorValClassObjectConflict(val test: String) {
    companion object {
        val test = { 12 }()
    }

    val some = test
}

class TestClassObjectAndClassConflict {
    companion object {
        val bla = { 12 }()
    }

    val bla = { "More" }()

    val some = bla
}
