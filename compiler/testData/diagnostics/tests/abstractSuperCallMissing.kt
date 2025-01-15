// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-73585

class Foo

interface Bar {
    fun buzz()
}

val a = object : Bar {
    override fun buzz() {
        super.<!ABSTRACT_SUPER_CALL!>buzz<!>()
    }
}