// KT-74654
// KT-89821: types of destructuring entries have no FIR yet
// LOOK_UP_FOR_ELEMENT_OF_TYPE: KtTypeReference
class C {
    val (a: <expr>Int</expr>, b) = 1 to 2
}
