package nonRoot
import java.lang.*

fun foo() {
    <caret>Fake()
}

//REF: (in java.lang.Fake).Fake()