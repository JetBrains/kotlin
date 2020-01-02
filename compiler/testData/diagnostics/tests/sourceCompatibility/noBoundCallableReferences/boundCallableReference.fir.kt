// !LANGUAGE: -BoundCallableReferences

class C { companion object }
val ok1 = C::hashCode
val fail1 = C.Companion::hashCode

object O {
    class Y {
        companion object
    }
}
val fail2 = O::hashCode
val ok2 = O::Y
val ok3 = O.Y::hashCode

enum class E {
    Entry
}
val ok4 = E.Entry::hashCode

fun hashCode() {}

val fail3 = ""::hashCode
val fail4 = (C)::hashCode
val fail5 = (C.Companion)::hashCode
