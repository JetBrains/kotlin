// "Change function signature to 'fun next(bits: Int): Int'" "true"
import java.util.Random

class MyRandom : Random() {
    <caret>override fun next(): Int = 4
}
