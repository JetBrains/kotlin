// WITH_STDLIB
// ISSUE: KT-53349

class SomeClass

val SomeClass.lore: List<String>
    get() {
        apply {
            return emptyList()
        }
    }
