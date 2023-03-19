// ISSUE: KT-37490

interface SomeFace<T>

class SomeGene<T> {
    fun setFace(face: SomeFace<T>?) {}
    fun setString(string: String?) {}
}

fun nullTypeArg(invP: SomeGene<Any>, inP: SomeGene<in Any>, outP: SomeGene<out Any>) {
    invP.setFace(null)
    invP.setString(null)
    inP.setFace(null)
    inP.setString(null)
    outP.setFace(null)
    outP.setString(null)
}
