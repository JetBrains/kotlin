// MODULE: lib
// MODULE_KIND: LibrarySource
// LANGUAGE: +CompanionBlocksAndExtensions +CollectionLiterals
// RUN_PIPELINE_TILL: BACKEND

// FILE: MyCollection.kt

package test

class MyCollection {
    companion {
        internal operator fun of(vararg i: Int): MyCollection = MyCollection()
    }
}

fun testInLib() {
    val collection: MyCollection = [1, 2, 3]
}

// MODULE: main(lib)
// LANGUAGE: +CompanionBlocksAndExtensions +CollectionLiterals
// WITH_STDLIB
// RUN_PIPELINE_TILL: FRONTEND

// FILE: main.kt

import test.MyCollection

fun testInMain() {
    val collection: MyCollection <!INITIALIZER_TYPE_MISMATCH!>=<!> [1, 2, 3]
    val collection2 = when {
        true -> MyCollection()
        else -> [1, 2, 3]
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, localProperty, operator,
propertyDeclaration, vararg */
