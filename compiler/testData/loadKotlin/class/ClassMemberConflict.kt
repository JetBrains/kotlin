package test

class ConstructorTypeParamClassObjectTypeConflict<test> {
    class object {
        trait test
    }

    val some: test? = null
}

class ConstructorTypeParamClassObjectConflict<test> {
    class object {
        val test = 12
    }

    val some = test
}

class TestConstructorParamClassObjectConflict(test: String) {
    class object {
        val test = 12
    }

    val some = test
}


class TestConstructorValClassObjectConflict(val test: String) {
    class object {
        val test = 12
    }

    val some = test
}

class TestClassObjectAndClassConflict {
    class object {
        val bla = 12
    }

    val bla = "More"

    val some = bla
}