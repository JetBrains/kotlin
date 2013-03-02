// "Change function signature to 'protected override fun next(p0: Int): Int'" "true"
import java.util.Random

class MyRandom : Random() {
    <caret>protected override fun next(p0: Int): Int = 4
}