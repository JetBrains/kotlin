// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE
[file: Ann]
annotation class Ann(val arg: Int = 1)

fun bar(block: () -> Int) = block()

data class Q(val x: Int, val y: Int)

fun bar2(): Array<Q> = null!!

<!DEPRECATED_ANNOTATION_SYNTAX!>[Ann Ann]<!> class A [Ann] (<!DEPRECATED_ANNOTATION_SYNTAX!>[Ann]<!> val prop: Int) {
    <!DEPRECATED_ANNOTATION_SYNTAX!>[Ann]<!> val x = 1
    <!DEPRECATED_ANNOTATION_SYNTAX!>[Ann]<!> fun foo(<!DEPRECATED_ANNOTATION_SYNTAX!>[Ann]<!> x: Int) {
        <!DEPRECATED_ANNOTATION_SYNTAX!>[Ann]<!> class B

        <!DEPRECATED_ANNOTATION_SYNTAX!>[Ann]<!> fun local() {}

        2 + <!DEPRECATED_ANNOTATION_SYNTAX!>[Ann Ann]<!> 2

        for (<!DEPRECATED_ANNOTATION_SYNTAX!>[Ann]<!> (a, <!DEPRECATED_ANNOTATION_SYNTAX!>[Ann]<!> b) in bar2()) {}
    }

    fun x(): <!DEPRECATED_ANNOTATION_SYNTAX!>[Ann]<!> String {
        <!DEPRECATED_ANNOTATION_SYNTAX!>[Ann]<!> val x: <!DEPRECATED_ANNOTATION_SYNTAX!>[Ann]<!> String = ""
        return ""
    }
}

val y: Array<[<!DEBUG_INFO_MISSING_UNRESOLVED!>Ann<!>] String?> = arrayOfNulls(1)
val block: ([<!DEBUG_INFO_MISSING_UNRESOLVED!>Ann<!>] x: <!DEPRECATED_ANNOTATION_SYNTAX!>[Ann]<!> String) -> <!DEPRECATED_ANNOTATION_SYNTAX!>[Ann]<!> String = { "" }
interface B
interface D : <!DEPRECATED_ANNOTATION_SYNTAX!>[Ann]<!> B

Ann(<!ANNOTATION_PARAMETER_MUST_BE_CONST!><!DEPRECATED_ANNOTATION_SYNTAX!>[Ann]<!> 1<!>) class MyClass
