// API_VERSION: 1.8
// !DIAGNOSTICS: -INVISIBLE_MEMBER -INVISIBLE_REFERENCE

@RequiresOptIn
@Retention(AnnotationRetention.BINARY)
annotation class ClassMarker

@RequiresOptIn
@Retention(AnnotationRetention.BINARY)
annotation class ConstructorMarker

@RequiresOptIn
@Retention(AnnotationRetention.BINARY)
annotation class TypeAliasMarker

@SinceKotlin("1.9")
@WasExperimental(ClassMarker::class)
class C {
    @SinceKotlin("1.9")
    @WasExperimental(ConstructorMarker::class)
    constructor() {}
}

@SinceKotlin("1.9")
@WasExperimental(TypeAliasMarker::class)
typealias T = <!OPT_IN_USAGE_ERROR!>C<!>

@ClassMarker
fun test1() {
    <!OPT_IN_USAGE_ERROR!>C<!>()
}

@ConstructorMarker
fun test2() {
    <!OPT_IN_USAGE_ERROR!>C<!>()
}

@ClassMarker
@ConstructorMarker
fun test3() {
    C()
}

@ClassMarker
fun test4(t: <!OPT_IN_USAGE_ERROR!>T<!>) {}

@TypeAliasMarker
fun test5(t: <!OPT_IN_USAGE_ERROR!>T<!>) {}

@ClassMarker
@TypeAliasMarker
fun test6(t: T) {}
