// !DIAGNOSTICS: -UNUSED_PARAMETER

interface I

class A : I by impl {

    companion object {
        val impl = object : I {}
    }
}
