package test

fun box() = IssueState.DEFAULT.ToString() + IssueState.FIXED.ToString()

enum class IssueState {
   DEFAULT {
       inner class D {
          val k = ToString()
       }
   },
   FIXED {
       override fun ToString() = "K"

       inner class D {
          val k = ToString()
       }
   };

   open fun ToString() : String = "O"
}
