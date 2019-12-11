interface Trait<N : Number>

object O1 : Trait<Int>

object O2 : Trait<String>

class C {
    companion object : Trait<IntRange>
}
