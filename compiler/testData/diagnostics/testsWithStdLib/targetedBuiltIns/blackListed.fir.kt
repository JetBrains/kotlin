// !WITH_NEW_INFERENCE
// FULL_JDK

abstract class A : MutableList<String> {
    override fun sort(/*0*/ p0: java.util.Comparator<in String>) {
        super.sort(p0)
    }
}

fun foo(x: MutableList<String>, y: java.util.ArrayList<String>, z: A, p: java.util.Comparator<in String>) {
    x.sort(p)
    y.sort(p)

    z.sort(p)
}

fun bar(x: MutableList<String>, y: java.util.ArrayList<String>, z: A) {
    x.sort { a, b -> a.length - b.length }
    y.sort { a, b -> a.length - b.length }

    z.sort { a, b -> a.length - b.length }
}
