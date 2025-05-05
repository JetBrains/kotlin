// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-17417, KT-46313

interface SomeInterface {
    val x : Int
}

enum class EEE {
    A, B, C;

    companion object : SomeInterface by EEE
}

class Some {
    companion object : SomeInterface by Some
}
