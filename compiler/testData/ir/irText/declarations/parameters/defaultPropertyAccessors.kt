// FIR_IDENTICAL

// MUTE_SIGNATURE_COMPARISON_K2: ANY
// ^ KT-57429

val test1 = 42

var test2 = 42

class Host {
    val testMember1 = 42

    var testMember2 = 42
}

class InPrimaryCtor<T>(
        val testInPrimaryCtor1: T,
        var testInPrimaryCtor2: Int = 42
)
