package beforeTest //package name should be less than imported one

import test.*

fun main(args: Array<String>) {
    "O".switchMapOnce {

        "K".switchMapOnce {
            "OK"
        }
    }
}