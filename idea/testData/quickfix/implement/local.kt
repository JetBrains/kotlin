// "class org.jetbrains.kotlin.idea.intentions.CreateKotlinSubClassIntention" "false"

fun foo() {
    abstract class <caret>My {
        abstract fun bar(): Int
    }
}

