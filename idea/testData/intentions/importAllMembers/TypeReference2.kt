// INTENTION_TEXT: "Import members from 'com.test.States'"
import com.test.States

fun foo(s: States) {
    when (s) {
        is States.Loading -> {
        }
        is States.Error -> {
        }
        is States.Content -> {
        }
    }
}

fun bar(): <caret>States.Loading = States.Loading