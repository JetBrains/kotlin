// ISSUE: KT-59584

class Klass {
    val maybeThis = if (1 == 1) this else null
}
