import java.*
import java.util.*

fun test() {
  Collections.singleton<Int>(<error>1.0</error>)
  Collections.singleton<Int>(<error>2.0</error>)
  Collections.singleton<Int>(<error>3.0</error>)
  Collections.singleton<Int>(<error>4.0</error>)
  Collections.singleton<Int>(<error>5.0</error>)
  Collections.singleton<Int>(<error>6.0</error>)
}
