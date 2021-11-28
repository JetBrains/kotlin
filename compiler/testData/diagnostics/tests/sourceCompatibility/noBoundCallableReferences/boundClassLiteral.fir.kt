// !LANGUAGE: -BoundCallableReferences

class C { companion object }
val ok1 = C::class
val ok2 = C.Companion::class

object O
val ok3 = O::class

enum class E {
    Entry
}
val ok4 = E.Entry::class

val fail1 = ""::class
val fail2 = <!NULLABLE_TYPE_IN_CLASS_LITERAL_LHS!>String?::class<!>
val fail3 = (C)::class
val fail4 = (C.Companion)::class
