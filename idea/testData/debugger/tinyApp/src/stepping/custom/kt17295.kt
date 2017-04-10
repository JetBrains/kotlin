package kt17295

import java.lang.Thread.sleep
import kotlin.concurrent.timer

fun main(args: Array<String>) {
    timer("Repeating println", period=100) {
        //Breakpoint!
        foo()
        System.exit(0)
    }

    sleep(2000)
}

fun foo() {}

// RESUME: 1