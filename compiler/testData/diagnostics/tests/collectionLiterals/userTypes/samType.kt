// ISSUE: KT-85497
// LANGUAGE: +CollectionLiterals, +CompanionBlocksAndExtensions
// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

// FILE: test/JavaSamNoOf.java

package test;

@FunctionalInterface
interface JavaSamNoOf {
    void foo();
}

// FILE: test/MySam.kt
package test

fun interface MySam {
    fun foo()

    companion object {
        operator fun of(vararg ints: Int): MySam = { }
    }
}

fun <T> id(t: T): T = t

fun takeSam(mySam: MySam) { }
fun runSam(block: () -> MySam) { }

private fun test() {
    val mySam: MySam = []
    takeSam([1, 2, 3])
    runSam { [1, 2, 3] }
    runSam { [<!ARGUMENT_TYPE_MISMATCH!>"!"<!>] }

    val mySamWithId: MySam = id([])
    takeSam(id([1, 2, 3]))
    runSam { id([1, 2, 3]) }
    runSam(id { id([1, 2, 3]) })
    runSam { id([<!ARGUMENT_TYPE_MISMATCH!>"!"<!>]) }
}

// FILE: test/GenericSam.kt
package test

fun interface MyGenericSam<T> {
    fun foo(): T?

    companion {
        operator fun <T> of(vararg subtasks: MyGenericSam<T>): MyGenericSam<T> = {
            var res: T? = null
            for (task in subtasks) {
                res = task.foo()
            }
            res
        }
    }
}

fun <K> takeGenericSam(sam: MyGenericSam<K>) { }

fun <D> materialize(): D = null!!

private fun test() {
    takeGenericSam<String>([])
    takeGenericSam<String>([[]])
    <!CANNOT_INFER_PARAMETER_TYPE!>takeGenericSam<!>(<!CANNOT_INFER_PARAMETER_TYPE!>[]<!>)
    takeGenericSam([{ "!" }])
    takeGenericSam([{ "!" }, { materialize() }])
    takeGenericSam([[ { materialize() } ], { "!" }])
    takeGenericSam<Int>([{ <!RETURN_TYPE_MISMATCH!>42L<!> }])
}

// FILE: test/noOf.kt
package test

fun interface NoOf {
    fun foo(): Int
}

private fun test() {
    val noOf: NoOf <!INITIALIZER_TYPE_MISMATCH!>=<!> [1, 2, 3]
    id<NoOf>(<!ARGUMENT_TYPE_MISMATCH!>[1, 2, 3]<!>)

    val javaNoOf: JavaSamNoOf <!INITIALIZER_TYPE_MISMATCH!>=<!> [1, 2, 3]
    id<JavaSamNoOf>(<!ARGUMENT_TYPE_MISMATCH!>[1, 2, 3]<!>)
}

/* GENERATED_FIR_TAGS: collectionLiteral, companionObject, funInterface, functionDeclaration, functionalType,
integerLiteral, interfaceDeclaration, lambdaLiteral, localProperty, objectDeclaration, operator, propertyDeclaration,
samConversion, vararg */
