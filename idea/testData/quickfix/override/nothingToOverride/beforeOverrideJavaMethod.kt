// "Change function signature to 'fun next(p0: Int): Int'" "true"
import java.util.Random

class MyRandom : Random() {
    <caret>override fun next(): Int = 4
}