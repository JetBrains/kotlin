trait Trait<N : Number>

object O1 : Trait<Int>

object O2 : Trait<<!UPPER_BOUND_VIOLATED!>String<!>>

class C {
    default object : Trait<<!UPPER_BOUND_VIOLATED!>IntRange<!>>
}
