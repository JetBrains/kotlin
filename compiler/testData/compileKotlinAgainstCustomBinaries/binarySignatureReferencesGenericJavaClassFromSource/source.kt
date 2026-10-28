import p.Gen
import p.Ref

fun test(ref: Ref): String {
    val s: String = ref.get().value
    val g: Gen<String> = ref.raw()
    return s + g.value
}
