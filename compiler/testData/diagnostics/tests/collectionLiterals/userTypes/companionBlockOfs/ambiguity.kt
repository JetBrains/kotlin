// LANGUAGE: +CompanionBlocksAndExtensions +CollectionLiterals
// RUN_PIPELINE_TILL: FRONTEND

class A {
    companion {
        operator fun of(vararg x: Int): A = A()
    }
}

class B {
    companion {
        operator fun of(vararg x: Int): B = B()
    }
}

class C {
    companion object {
        operator fun of(vararg x: Int): C = C()
    }
}

fun Int.test() {
    val x = when (this) {
        0 -> A()
        1 -> B()
        else -> <!AMBIGUOUS_COLLECTION_LITERAL!>[1, 2, 3]<!>
    }

    val y = when (this) {
        0 -> A()
        1 -> C()
        else -> <!AMBIGUOUS_COLLECTION_LITERAL!>[1, 2, 3]<!>
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, equalityExpression, funWithExtensionReceiver,
functionDeclaration, integerLiteral, localProperty, objectDeclaration, operator, propertyDeclaration, vararg,
whenExpression, whenWithSubject */
