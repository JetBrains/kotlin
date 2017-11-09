@Target(AnnotationTarget.TYPE)
annotation class Ann

typealias TString = String

typealias TNString = TString?

typealias TNAString = @Ann TString?

val test1: TNString = TODO()
val test2: TNAString = TODO()
val test3: List<TNString> = TODO()
val test4: List<TNAString> = TODO()
val test5: List<TNString<!REDUNDANT_NULLABLE!>?<!>> = TODO()
val test6: () -> List<TNString> = TODO()

fun test(x: TNString) {
    x<!UNSAFE_CALL!>.<!>hashCode()
}
