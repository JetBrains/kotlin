// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtClass
// MAIN_FILE_NAME: Foo
package test

typealias MySuspendAlias <T> = suspend (Int, suspend (String) -> T) -> T

class Foo {
    val explicitInt: MySuspendAlias<Int>
        get() = null!!

    val explicitAny: MySuspendAlias<Any?>
        get() = null!!

    val explicitStar: MySuspendAlias<*>
        get() = null!!
}
