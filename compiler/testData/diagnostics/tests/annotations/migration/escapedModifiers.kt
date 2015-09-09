// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE -NOTHING_TO_INLINE -NOT_YET_SUPPORTED_IN_INLINE
import kotlin.external as myNative
<!DEPRECATED_ANNOTATION_THAT_BECOMES_MODIFIER!>@annotation<!> <!DEPRECATED_ANNOTATION_THAT_BECOMES_MODIFIER!>@data<!> <!DEPRECATED_ESCAPED_MODIFIER!>@public<!> class Ann(val arg: Int = 1)


<!DEPRECATED_ANNOTATION_THAT_BECOMES_MODIFIER!>@inline<!> <!DEPRECATED_ESCAPED_MODIFIER!>@private<!> fun bar(block: () -> Int) = block()

<!DEPRECATED_ANNOTATION_THAT_BECOMES_MODIFIER!>@data<!> class Q(val x: Int, val y: Int)

fun bar2(): Array<Q> = null!!

<!DEPRECATED_ESCAPED_MODIFIER!>@open<!> class A <!DEPRECATED_ESCAPED_MODIFIER!>@private<!> constructor(<!DEPRECATED_ESCAPED_MODIFIER!>@private<!> val prop: Int) {
    <!DEPRECATED_ESCAPED_MODIFIER!>@private<!> val x = 1
    <!DEPRECATED_ANNOTATION_THAT_BECOMES_MODIFIER!>@inline<!> fun foo(<!DEPRECATED_ANNOTATION_THAT_BECOMES_MODIFIER!>@noinline<!> x: Int) {
        <!DEPRECATED_ANNOTATION_THAT_BECOMES_MODIFIER!>@data<!> class Local

        <!DEPRECATED_ANNOTATION_THAT_BECOMES_MODIFIER!>@inline<!> fun localFun() {}
    }

    <!DEPRECATED_ESCAPED_MODIFIER!>@private<!> object O1 {}
    <!DEPRECATED_ESCAPED_MODIFIER!>@public<!> <!DEPRECATED_ESCAPED_MODIFIER!>@companion<!> object O2 {}
}

<!DEPRECATED_ANNOTATION_THAT_BECOMES_MODIFIER!>kotlin.inline<!> fun baz() { }
<!DEPRECATED_ANNOTATION_THAT_BECOMES_MODIFIER!>kotlin.data<!> class Data

<!DEPRECATED_ANNOTATION_THAT_BECOMES_MODIFIER!>myNative<!> fun nativeFun(): Int

<!DEPRECATED_ANNOTATION_THAT_BECOMES_MODIFIER!>@tailRecursive<!> fun tailFun(): Int = tailFun()

inline fun inlineFun(<!DEPRECATED_ANNOTATION_THAT_BECOMES_MODIFIER!>@inlineOptions(InlineOption.ONLY_LOCAL_RETURN)<!> block: () -> Int) {}
inline fun inlineFun2(<!DEPRECATED_ANNOTATION_USE!>@inlineOptions(InlineOption.LOCAL_CONTINUE_AND_BREAK)<!> block: () -> Int) {}
