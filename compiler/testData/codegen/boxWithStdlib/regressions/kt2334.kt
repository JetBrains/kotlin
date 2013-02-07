import kotlin.test.*
import org.junit.Test as test

public class Test {
    test fun f(): Unit {
        assertEquals(true, !false)
    }
}

fun box() : String {
  Test().f()
  return "OK"
}
