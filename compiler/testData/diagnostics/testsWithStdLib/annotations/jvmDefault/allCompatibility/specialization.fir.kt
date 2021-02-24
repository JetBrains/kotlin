// !JVM_DEFAULT_MODE: all-compatibility
// JVM_TARGET: 1.8
// WITH_RUNTIME

interface Foo<T> {
    fun test(p: T) = "fail"
    val T.prop: String
        get() = "fail"
}

interface FooDerived: Foo<String>

class Unspecialized<Y> : Foo<Y>

open class UnspecializedFromDerived : FooDerived

abstract class AbstractUnspecializedFromDerived : FooDerived

open class Specialized : Foo<String>

abstract class AbstractSpecialized : Foo<String>


@JvmDefaultWithoutCompatibility
open class UnspecializedFromDerivedNC : FooDerived

@JvmDefaultWithoutCompatibility
abstract class AbstractUnspecializedFromDerivedNC : FooDerived

@JvmDefaultWithoutCompatibility
open class SpecializedNC : Foo<String>

@JvmDefaultWithoutCompatibility
abstract class AbstractSpecializedNC : Foo<String>


final class FinalSpecialized : Foo<String>

sealed class SealedSpecialized : Foo<String> {
    open class A : SealedSpecialized();
}

enum class EnumSpecialized : Foo<String> {
     ENTRY {
         fun test() = 123
     }
}

object ObjectSpecialized : Foo<String>

private class Outer {

    open class InnerSpecialized: Foo<String>
}

fun local() {
    object : Foo<String> {}
}

fun interface F : Foo<String> {
    fun invoke(o: String): String
}

fun test(): String {
    if (F { o -> o + "K" }.invoke("O") != "OK") return "Fail"

    val lambda: (String) -> String = { o -> o + "K" }
    return F(lambda).invoke("O")
}
