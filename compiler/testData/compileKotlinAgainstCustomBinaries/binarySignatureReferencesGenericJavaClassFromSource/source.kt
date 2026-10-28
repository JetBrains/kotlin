import p.Ref

fun test(ref: Ref): String {
    val s: String = ref.get().value
    return s
}
