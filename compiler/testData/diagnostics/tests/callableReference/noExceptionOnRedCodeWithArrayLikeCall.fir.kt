// !LANGUAGE: +NewInference

class DTO {
    val q: Int = 0
    operator fun get(prop: KProperty1<*, Int>): Int = 0
}

fun foo(intDTO: DTO?, p: KProperty1<*, Int>) {
    if (intDTO != null) {
        intDTO[DTO::q]
        intDTO.q
    }
}
