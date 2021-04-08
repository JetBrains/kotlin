// !LANGUAGE: +ProperCheckAnnotationsTargetInTypeUsePositions
// ISSUE: KT-25876

@Target(AnnotationTarget.TYPE)
annotation class Anno(val value: String)

fun foo(x: String): @Anno(Lorem, ipsum::class, "dolor", sit-amet) String {  // OK
    return x
}

abstract class Foo : @Anno(o_O) Throwable()  // OK

abstract class Bar<T : @Anno(O_o) Any>  // OK
