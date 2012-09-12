package test

fun box() = IssueState.DEFAULT.ToString() + IssueState.FIXED.ToString()

enum class IssueState {
   DEFAULT {
       class D {
          val k = ToString()
       }
   }
   FIXED {
       override fun ToString() = "K"

       object D {
          val k = ToString()
       }
   }

   open fun ToString() : String = "O"
}