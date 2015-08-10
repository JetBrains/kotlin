fun box() = IssueState.DEFAULT.ToString() + IssueState.FIXED.ToString()

enum class IssueState {
   DEFAULT,
   FIXED {
       override fun ToString() = "K"
   };

   open fun ToString() : String = "O"
}
