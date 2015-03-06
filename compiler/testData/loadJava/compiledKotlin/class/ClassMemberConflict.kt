//ALLOW_AST_ACCESS
package test

class ConstructorTypeParamClassObjectTypeConflict<test> {
    default object {
        trait test
    }

    val some: test? = throw Exception()
}

class ConstructorTypeParamClassObjectConflict<test> {
    default object {
        val test = 12
    }

    val some = test
}

class TestConstructorParamClassObjectConflict(test: String) {
    default object {
        val test = 12
    }

    val some = test
}


class TestConstructorValClassObjectConflict(val test: String) {
    default object {
        val test = 12
    }

    val some = test
}

class TestClassObjectAndClassConflict {
    default object {
        val bla = 12
    }

    val bla = "More"

    val some = bla
}