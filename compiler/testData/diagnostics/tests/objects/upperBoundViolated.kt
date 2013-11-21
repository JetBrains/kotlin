trait Trait<N : Number>

object O1 : Trait<Int>

object O2 : Trait<<!UPPER_BOUND_VIOLATED!>String<!>>

class C {
    class object : Trait<<!UPPER_BOUND_VIOLATED!>IntRange<!>>
}
